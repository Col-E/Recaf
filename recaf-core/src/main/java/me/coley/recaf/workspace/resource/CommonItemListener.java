package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.ItemInfo;

/**
 * Listener for receiving item update events.
 *
 * @param <I>
 * 		Item type implementation. Either {@link ClassInfo} or {@link FileInfo}.
 *
 * @author Matt Coley
 */
public abstract class CommonItemListener<I extends ItemInfo> {
	/**
	 * Delegate to the class listener.
	 *
	 * @param listener
	 * 		Class listener.
	 *
	 * @return Wrapper delegate listener.
	 */
	public static CommonItemListener<ClassInfo> wrapClass(ResourceClassListener listener) {
		return new CommonItemListener<ClassInfo>() {
			@Override
			void onNewItem(Resource resource, ClassInfo newValue) {
				listener.onNewClass(resource, newValue);
			}

			@Override
			void onRemoveItem(Resource resource, ClassInfo oldValue) {
				listener.onRemoveClass(resource, oldValue);
			}

			@Override
			void onUpdateItem(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
				listener.onUpdateClass(resource, oldValue, newValue);
			}
		};
	}

	/**
	 * Delegate to the dex class listener.
	 *
	 * @param dexName
	 * 		Name of affected dex file in a resource.
	 * @param listener
	 * 		Dex class listener.
	 *
	 * @return Wrapper delegate listener.
	 */
	public static CommonItemListener<DexClassInfo> wrapDex(String dexName, ResourceDexClassListener listener) {
		return new CommonItemListener<DexClassInfo>() {
			@Override
			void onNewItem(Resource resource, DexClassInfo newValue) {
				listener.onNewDexClass(resource, dexName, newValue);
			}

			@Override
			void onRemoveItem(Resource resource, DexClassInfo oldValue) {
				listener.onRemoveDexClass(resource, dexName, oldValue);
			}

			@Override
			void onUpdateItem(Resource resource, DexClassInfo oldValue, DexClassInfo newValue) {
				listener.onUpdateDexClass(resource, dexName, oldValue, newValue);
			}
		};
	}

	/**
	 * Delegate to the file listener.
	 *
	 * @param listener
	 * 		File listener.
	 *
	 * @return Wrapper delegate listener.
	 */
	public static CommonItemListener<FileInfo> wrapFile(ResourceFileListener listener) {
		return new CommonItemListener<FileInfo>() {
			@Override
			void onNewItem(Resource resource, FileInfo newValue) {
				listener.onNewFile(resource, newValue);
			}

			@Override
			void onRemoveItem(Resource resource, FileInfo oldValue) {
				listener.onRemoveFile(resource, oldValue);
			}

			@Override
			void onUpdateItem(Resource resource, FileInfo oldValue, FileInfo newValue) {
				listener.onUpdateFile(resource, oldValue, newValue);
			}
		};
	}

	/**
	 * Called when a new value is added.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param newValue
	 * 		Item added to the resource.
	 */
	abstract void onNewItem(Resource resource, I newValue);

	/**
	 * Called when an old value is removed.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param oldValue
	 * 		Item removed from the resource.
	 */
	abstract void onRemoveItem(Resource resource, I oldValue);

	/**
	 * Called when the old value is replaced by the new one.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param oldValue
	 * 		Prior item value.
	 * @param newValue
	 * 		New item value.
	 */
	abstract void onUpdateItem(Resource resource, I oldValue, I newValue);
}
