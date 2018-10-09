package de.lhug.webflow.builder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.config.FlowDefinitionResourceFactory;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.engine.ActionState;
import org.springframework.webflow.engine.EndState;

public class FlowDefinitionBuilderTest {

	private FlowDefinitionBuilder sut;

	private void initSut(String flowPath) {
		FlowDefinitionResource resource = createFlowDefinitionResource(flowPath);
		sut = new FlowDefinitionBuilder(resource);
	}

	private FlowDefinitionResource createFlowDefinitionResource(String path) {
		String resourcePath = getClass().getResource(path).toExternalForm();
		return new FlowDefinitionResourceFactory().createResource(resourcePath);
	}

	@Test
	public void shouldCreateFlowDefinitionFromResource() throws Exception {
		initSut("/exampleFlow.xml");

		FlowDefinition result = sut.build();

		assertThat(result.getId(), is("exampleFlow"));
		assertThat(result.getStartState().getId(), is("start"));
		assertThat(result.getState("bye"), is(instanceOf(EndState.class)));
	}

	@Test
	public void shouldRegisterDependenFlowModels() throws Exception {
		FlowDefinitionResource parentResource = createFlowDefinitionResource("/inheritanceFlows/parentFlow.xml");
		initSut("/inheritanceFlows/childFlow.xml");

		sut.addParent(parentResource);

		assertThat(sut.getDependentFlowModels(), contains(parentResource));
	}

	@Test
	public void shouldBuildFlowDefinitionWithParentFlow() throws Exception {
		FlowDefinitionResource parentResource = createFlowDefinitionResource("/inheritanceFlows/parentFlow.xml");
		initSut("/inheritanceFlows/childFlow.xml");

		FlowDefinition result = sut.addParent(parentResource)
				.build();

		assertThat(result.getStartState().getId(), is("child-entry"));
		// "motherKnowsBest" state defined in parent flow
		assertThat(result.getState("motherKnowsBest"), is(instanceOf(ActionState.class)));
	}

	@Test
	public void shouldRegisterBeanInContext() throws Exception {
		SomeBean bean = new SomeBean();
		initSut("/simpleFlows/flowWithDependentBeans.xml");

		sut.addBean("myBean", bean);

		assertThat(sut.getDependentBeans().get("myBean"), sameInstance(bean));
	}

	public static class SomeBean {
		public String getMessage() {
			return "moo";
		}
	}

	@Test
	public void shouldRegisterBeanInContextWithGeneratedBeanName() throws Exception {
		SomeBean bean = new SomeBean();
		initSut("/simpleFlows/flowWithDependentBeans.xml");

		sut.addBean(bean);

		assertThat(sut.getDependentBeans().get("someBean"), sameInstance(bean));
	}

	@Test
	public void shouldBuildFlowWithDependentBeans() throws Exception {
		SomeBean bean = new SomeBean();
		initSut("/simpleFlows/flowWithDependentBeans.xml");
		sut.addBean(bean);

		FlowDefinition result = sut.build();

		assertThat(result.getApplicationContext().getBean("someBean"), sameInstance(bean));
	}
}
