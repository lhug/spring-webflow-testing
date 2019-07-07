# Spring Webflow Testing

A utility library to allow simple testing of Spring WebFlows

## Disclaimer

This library is created with `JUnit 4.12` and `spring WebFlow 2.3.3-RELEASE`, utilizing JDK 8. So far, no guarantees are made about using it on other JDK versions or dependency versions.

## Overview

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=lhug_spring-webflow-testing&metric=alert_status)](https://sonarcloud.io/dashboard?id=lhug_spring-webflow-testing)

The Tester itself can directly run a Flow and allow assertions on it, as well as exposing several convenience getters.
It automatically creates a new ExternalContext instance which is passed to the flow on every request, meaning that it is possible to follow the entire flow from start to end without worrying if certain actions are not executed.
The Tester is being created by passing an instance of `de.lhug.webflowtester.builder.MockFlowBuilder`, a convenience interface exposing a single method returning a Flow instance.

The library also offers an `XMLMockFlowBuilder` to build a testable flow definition from an XML resource, a `FlowTestContext` containing Beans and SubFlows, and a `StubFlow`-class which can be used to stub SubFlows.

## Supported features

* Databinding
* Validation
* Localized messages
* Spring-Beans
* Flow-inheritance
* Passing input attributes at flow and view level
* Accessing output attributes
* Starting flow at specific state
* Easy loading of XML-flows
* Easy mocking of Subflows

## Restrictions

* Currently there is no way and no plan to support global flow attributes.
* Currently there is no way of adding a preconfigured Spring-Context as provided by using the `SpringRunner`
* Springs `Validator`-Bean is **not** being automatically instantiated. It can, however, be added manually.
* All messages, that do not provide a default text, **must** be added explicitly. If not, a `NoSuchMessageException` is raised during runtime.

## Usage

### de.lhug.webflowtester.builder.configuration.FlowTestContext

This class holds all runtime beans and subflows that will be added by the MockFlowBuilder instance. It utilizes the `Conventions` class offered by Spring to automatically generate bean names, and exposes a convenience constructor accepting a vararg of objects which will all be registered as flow beans and passed to the Flows internal `ApplicationContext`.

This can be done like so:

```{java}
Object bean1 = //init bean
Mock bean2 = //init mock

FlowTestContext context = new FlowTestContext(bean1, bean2);
```

It is, of course, possible to set the value directly, or to set a value with a specific id, by using the exposed `addBean(String, Object)` method.  
Likwewise, this allows the addition of any kind of `FlowDefinitionHolder` as subflow, utilizing `addSubFlow(FlowDefinitionHolder)`. Plus, some extra state checking methods are present as well, such as `containsBean(String)` which allows to check if a bean has already been registered.

Single localized messages can be added directly by using `context.addMessage(Locale, Key, Value)`, while successive messages can be added either builder-style or via a map containine all message keys and interpolations:

```{java}
Map<String, String> messages = // init messages
context.addMessages(Locale, messages);

// or

context.getMessages(Locale)
  .addMessage(key, value)
  .addMessage(otherKey, otherValue);
```

### de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration

This class holds all model information for the XMLFlowBuilder. It is being initialized by passing a resource locating object to the constructor. This is being examined and it is tried to determine which kind of resource this object denotes.
Specifics on the lookup and decision mechanism can be found on the javadoc.
This also contains Resource locations for required flows, such as parent flows. This can set a basePath directive so that the id of the flows will be determined by it. See Javadoc for `FlowDefinitionResourceFactory.getFlowId()`

Usage:

```{java}
new XMLMockFlowConfiguration("/src/test/resources/simpleFlows/standaloneFlow.xml"); // with String
new XMLMockFlowConfiguration(new File()); // with file resource
new XMLMockFlowConfiguration(new URL()); // with URL resource
new XMLMockFlowConfiguration(new MyObject()); // falls back to String, calling Objects.toString() on the offer
```

### de.lhug.webflowtester.builder.XMLMockFlowBuilder

Used to construct the actual Flow instance. It requires an instance of XMLMockFlowConfiguration to work on, as those are guaranteed to return the correct resources.
This can then be passed to the MockFlowTester, which in turn builds the actual flow and allows the execution.

```{java}
new XMLMockFlowBuilder(configuration);
```

It also accepts the configured `FlowTestContext`:

```{java}
new XMLMockFlowBuilder(configuration)
  .withContext(flowTestContext);
```

### de.lhug.webflowtester.stub.StubFlow

This class is an implementation of `FlowDefinitionHolder` and allows to create single Subflow instances, that can be changed after the flow has been build to retufn different values, effectively behaving like a Mock Object.
It also exposes Methods to access the input parameters passed to the flow, and the output parameters, which the flow should have emitted on ending.

#### Initializing a StubFlow

The StubFlow can be initialized passing its Flow Id and initial End State Id:

```{java}
StubFlow stub = new StubFlow("flowId", "endState");
```

The Flow-Id can not be changed after this has been registered, so it must be chosen with care. The end state id is, however entirely interchangeable. when calling `setEndState(String)`, the previous id is overwritten, and the next call to `getFlowDefinition` will result in a new instance of the Flow.

#### Accessing input attributes

Given that a registered StubFlow was called during execution, its input attributes can be accessed and asserted on:

```{java}
AttributeMap inputAttributes = stub.getInputAttributes();

assertThat(inputAttribures.get("clientId"), is("TK-241"));
```

After this has been called, the captured input arguments are cleared for the next request.

#### Setting output attributes

The StubFlow exposes an `addOutputAttribute` method, allowing to easily add a key-value pair as output attributes. They will be emitted when the subFlow hits its endState, which it does directly as the StartState is the defined EndState.

```{java}
stub.addOutputAttribute("behavedCorrectly", "no");
```

### de.lhug.webflowtester.executor.MockFlowTester

This is the main class to work with. It is being initialized with

```{java}
MockFlowTester tester = MockFlowTester.from(MockFlowBuilder);
```

and can directly be used to test a flow. Typically, a flow test follows these lines:

```{java}
MockFlowTester tester = MockFlowTester.from(config);

tester.startFlow(); // starts the actual flow execution

tester.assertCurrentStateIs("waiting"); // asserts the id of the current state
tester.setEventId("close"); // sets the event to be published on resume
tester.resumeFlow(); // continues the execution

tester.assertFlowExecutionHasEnded(); // asserts that the flow is inactive now
tester.assertFlowOutcomeIs("endState"); // asserts that the end state id is "endState"
```

#### Differences when calling start and resume

The `MockFlowTester` is most often called with `startFlow` or `resumeFlow`. Both methods accept an optional `Map<? extends String, ? extends Object>`.
Despite the fact, that both are named `inputArguments`, they have a different meaning. This is explained in the javadoc of the methods, but I feel that
it should be mentioned here as well.

When calling `startFlow`, the contents of the passed map will be used as **Flow Arguments**. They are being converted into an `AttributeMap`, which is in turn passed to the flow executor.
This means that all contents of the map are present in the `FlowScope` of the current Flow execution, and as such, can be read into an `<input>` directive
within the flow definition.

When calling `resumeFlow`, the contents of the passed map will be used as **Request Parameters**, meaning they will be usd as either `MultipartFile`, `String[]` or `String`.