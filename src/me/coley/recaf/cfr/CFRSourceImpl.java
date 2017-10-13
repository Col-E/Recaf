package me.coley.recaf.cfr;
import java.io.IOException;
import java.util.Collection;

import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

public class CFRSourceImpl implements ClassFileSource {
	/**
	 * Lookup assistor for inner classes and other references.
	 */
	private final CFRResourceLookup resources;

	public CFRSourceImpl(CFRResourceLookup resources) {
		this.resources = resources;
	}

	@Override
	public void informAnalysisRelativePathDetail(String s, String s1) {}

	@Override
	public Collection<String> addJar(String s) {
		throw new UnsupportedOperationException("Return paths of all classfiles in jar.");
	}

	@Override
	public String getPossiblyRenamedPath(String s) {
		return s;
	}

	@Override
	public Pair<byte[], String> getClassFileContent(String pathOrName) throws IOException {
		pathOrName = pathOrName.substring(0, pathOrName.length() - ".class".length());
		return Pair.make(resources.get(pathOrName), pathOrName);
	}
}
