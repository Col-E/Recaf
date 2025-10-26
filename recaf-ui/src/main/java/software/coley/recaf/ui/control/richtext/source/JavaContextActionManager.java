package software.coley.recaf.ui.control.richtext.source;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.services.source.AstResolveResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager for {@link JavaContextActionManager}. Allows context actions to be observed via listeners.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JavaContextActionManager {
	private final List<ResolveListener> resolveListeners = new CopyOnWriteArrayList<>();
	private final List<SelectListener> selectListeners = new CopyOnWriteArrayList<>();
	private final List<ResolveListener> resolveListenersView = Collections.unmodifiableList(resolveListeners);
	private final List<SelectListener> selectListenersView = Collections.unmodifiableList(selectListeners);

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addResolveListener(@Nonnull ResolveListener listener) {
		resolveListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeResolveListener(@Nonnull ResolveListener listener) {
		resolveListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addSelectListener(@Nonnull SelectListener listener) {
		selectListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeSelectListener(@Nonnull SelectListener listener) {
		selectListeners.remove(listener);
	}

	/**
	 * @return List of registered context resolution listeners.
	 */
	@Nonnull
	public List<ResolveListener> getResolveListeners() {
		return resolveListenersView;
	}

	/**
	 * @return List of registered selection listeners.
	 */
	@Nonnull
	public List<SelectListener> getSelectListeners() {
		return selectListenersView;
	}

	/**
	 * Listener for receiving context resolutions.
	 */
	public interface ResolveListener {
		/**
		 * @param result
		 * 		Resolution result.
		 * @param pos
		 * 		Text offset position.
		 */
		void onResolve(@Nonnull AstResolveResult result, int pos);
	}

	/**
	 * Listener for receiving {@link ClassMember} selections within the Java decompiler display.
	 */
	public interface SelectListener {
		/**
		 * @param memberPath
		 * 		Path of member selected.
		 */
		void onSelect(@Nonnull ClassMemberPathNode memberPath);
	}
}
