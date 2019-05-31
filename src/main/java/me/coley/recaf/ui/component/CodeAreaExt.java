package me.coley.recaf.ui.component;

import org.fxmisc.richtext.*;

import java.util.NoSuchElementException;

/**
 * Bad hack to prevent the UI freezing up. This bug occurs seemingly at random, I'll have to look
 * into it more to figure out the root cause.
 */
public class CodeAreaExt extends CodeArea {
	@Override
	public CharacterHit hit(double x, double y) {
		try {
			return super.hit(x, y);
		} catch(NoSuchElementException e) {
			// TODO: Remove usage of this class after figuring out the true cause of this exception
			return CharacterHit.insertionAt(0);
		}
	}
}
