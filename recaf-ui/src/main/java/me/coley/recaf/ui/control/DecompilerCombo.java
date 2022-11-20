package me.coley.recaf.ui.control;

import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import me.coley.recaf.RecafUI;
import me.coley.recaf.decompile.DecompileManager;
import me.coley.recaf.decompile.Decompiler;

import java.util.Optional;
import java.util.TreeSet;

/**
 * ComboBox for switching between available decompiler implementations.
 *
 * @author Matt Coley
 */
public class DecompilerCombo extends ComboBox<Decompiler> {
	public DecompilerCombo() {
		DecompileManager decompileManager = RecafUI.getController().getServices().getDecompileManager();
		setConverter(new StringConverter<>() {
			@Override
			public String toString(Decompiler decompiler) {
				return decompiler.getName();
			}

			@Override
			public Decompiler fromString(String name) {
				return decompileManager.get(name);
			}
		});
		getItems().addAll(new TreeSet<>(decompileManager.getRegisteredImpls()));

	}

	/**
	 * @param decompiler
	 * 		Value to select.
	 *
	 * @return {@code true} on success.
	 * {@code false} if the value is not contained within the {@link ComboBox}'s item colleciton.
	 */
	public boolean select(Decompiler decompiler) {
		if (getItems().contains(decompiler)) {
			getSelectionModel().select(decompiler);
			return true;
		}
		return false;
	}

	/**
	 * @param name
	 * 		Name of decompiler value to select.
	 *
	 * @return {@code true} on success.
	 * {@code false} if the value is not contained within the {@link ComboBox}'s item colleciton.
	 */
	public boolean select(String name) {
		Optional<Decompiler> match = getItems().stream()
				.filter(d -> d.getName().equals(name))
				.findFirst();
		if (match.isPresent()) {
			getSelectionModel().select(match.get());
			return true;
		}
		return false;
	}
}
