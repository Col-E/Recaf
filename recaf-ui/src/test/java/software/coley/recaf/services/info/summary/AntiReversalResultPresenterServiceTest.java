package software.coley.recaf.services.info.summary;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import software.coley.recaf.services.analysis.antitamper.AntiReversalAnalysisResult;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AntiReversalResultPresenterService}.
 */
@SuppressWarnings("unchecked") // mock for Class<T> calls (it doesn't like generics)
class AntiReversalResultPresenterServiceTest {
	@Test
	void discoversAndOrdersPresenters() {
		// Setup Instance<T> list for low/high priority presenters.
		//  - Lower priority values are higher priority.
		Instance<AntiReversalResultPresenter> discovered = mock(Instance.class);
		AntiReversalResultPresenter lowPriority = new TestPresenter("a", -100);
		AntiReversalResultPresenter highPriority = new TestPresenter("b", -90);
		when(discovered.iterator()).thenReturn(List.of(highPriority, lowPriority).iterator());

		// The List<Presenter> should be ordered by priority, then analyzer ID.
		AntiReversalResultPresenterService service = new AntiReversalResultPresenterService(discovered);
		assertEquals(List.of("a", "b"), service.getPresenters().stream()
				.map(AntiReversalResultPresenter::getAnalyzerId)
				.toList());
	}

	@Test
	void rejectsDuplicateAndRemovesRegisteredPresenter() {
		// Setup Instance<T> list for just one presenter, then attempt to register a duplicate.
		Instance<AntiReversalResultPresenter> discovered = mock(Instance.class);
		AntiReversalResultPresenter presenter = new TestPresenter("duplicate", 0);
		when(discovered.iterator()).thenReturn(List.of(presenter).iterator());

		// Registering it again should throw.
		AntiReversalResultPresenterService service = new AntiReversalResultPresenterService(discovered);
		assertThrows(IllegalArgumentException.class, () -> service.registerPresenter(new TestPresenter("duplicate", 0)));

		// Removing it should succeed, and then removing it again should fail.
		assertTrue(service.removePresenter(presenter));
		assertFalse(service.removePresenter(presenter));

		// After removal, the list of presenters should be empty.
		assertTrue(service.getPresenters().isEmpty());
	}

	private record TestPresenter(@Nonnull String getAnalyzerId, int priority) implements AntiReversalResultPresenter {
		@Override
		public int priority() {
			return priority;
		}

		@Nonnull
		@Override
		public Class<? extends AntiReversalAnalysisResult> getResultType() {
			return AntiReversalAnalysisResult.class;
		}

		@Override
		public boolean isApplicable(@Nonnull AntiReversalAnalysisResult result) {
			return true;
		}

		@Override
		public void appendSummary(@Nonnull Workspace workspace,
		                          @Nonnull WorkspaceResource resource,
		                          @Nonnull AntiReversalAnalysisResult result,
		                          @Nonnull SummaryConsumer consumer,
		                          @Nonnull Executor actionExecutor) {
			// no-op
		}
	}
}
