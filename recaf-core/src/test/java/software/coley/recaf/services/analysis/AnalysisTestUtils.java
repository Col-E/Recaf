package software.coley.recaf.services.analysis;

import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import com.google.devrel.gmscore.tools.apk.arsc.StringPoolChunk;
import com.google.devrel.gmscore.tools.apk.arsc.XmlAttribute;
import com.google.devrel.gmscore.tools.apk.arsc.XmlStartElementChunk;
import jakarta.annotation.Nonnull;
import software.coley.recaf.info.BinaryXmlFileInfo;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Util classes for analysis tests.
 */
class AnalysisTestUtils {
	private static final String CERTIFICATE_PEM_BASE64 =
			"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tDQpNSUlEWWpDQ0FrcWdBd0lCQWdJSkFJdW1rK3FGb1p5QU1BMEdDU3FHU0liM0RRRUJEQ" +
					"VVBTUY4eEN6QUpCZ05WDQpCQVlUQWxWVE1RMHdDd1lEVlFRSUV3UlVaWE4wTVEwd0N3WURWUVFIRXdSVVpYTjBNUTR3REFZRF" +
					"ZRUUtFd1ZTDQpaV05oWmpFTk1Bc0dBMVVFQ3hNRVZHVnpkREVUTUJFR0ExVUVBeE1LVW1WallXWWdWR1Z6ZERBZUZ3MHlOakE" +
					"xDQpNakl3TkRJeE16SmFGdzB6TmpBMU1Ua3dOREl4TXpKYU1GOHhDekFKQmdOVkJBWVRBbFZUTVEwd0N3WURWUVFJDQpFd1JV" +
					"WlhOME1RMHdDd1lEVlFRSEV3UlVaWE4wTVE0d0RBWURWUVFLRXdWU1pXTmhaakVOTUFzR0ExVUVDeE1FDQpWR1Z6ZERFVE1CR" +
					"UdBMVVFQXhNS1VtVmpZV1lnVkdWemREQ0NBU0l3RFFZSktvWklodmNOQVFFQkJRQURnZ0VQDQpBRENDQVFvQ2dnRUJBTCtuWn" +
					"I5TjFDZUJScUJuVDhhTUVTQlZNeEVmTExINVBocldncUZPcXEvbS9iT2wxaUUwDQpPbDRxS1RyUDByTlZUeHZ1ZTB5UkhvNC8" +
					"0VUVkMEh1OGx1VSt2dmw5L0xiUGFJWXlGT2pFV2tkTkN0clo4TzQrDQozODBoSlBlN0hJRGhZc3RHSS84T1pzRGhSZ0c0NkNN" +
					"VXVMenplTWE1UEVFL0FrZllCZUI0K0MyYUJvNVRKZFZFDQpyb21UUHE0TEd2WHc1Q084TnIxNGZOT1k4MXZ4dU8xNGF1eEJKM" +
					"zNGeXlidWRoR0xGSC8xZk1TZ0RyVkQ0TzdIDQpCZnZQTkZ6ZmpDMVRBM1hHRk9ISllPRDU4WE9QMnpPbkRXRk56WWdsa3l6cm" +
					"dlT0F0SFZtMmVwdlJ6aXVGNWVLDQpLN2l4ckNiUm9Kc24vRThsMENFN1lWemZ5SXFMM3JQcjlMMENBd0VBQWFNaE1COHdIUVl" +
					"EVlIwT0JCWUVGTkx5DQprMytVeGEyOUcvSm5NRUFKZmkydjBHeWFNQTBHQ1NxR1NJYjNEUUVCREFVQUE0SUJBUUFFN2VPMm5I" +
					"Qy80VXp6DQorU3FuNjVCOVNsd0M4UGl6VUdhUXZTUFlvYmNRdkN1NzdIYlo1dFpTcktPa1FGaTlFTXlzSmRHZEEwMDlUTXpVD" +
					"QpWNUErR2I0VWo0UU51Z1kyclp5VlArK284UmozTkhaSjFMc2o0bGZCUHpFcDVrVVBZNWdkMVgwMXRlb2x4Qks1DQp1aTF6c0" +
					"doR2Z6anZIRXZIdjBOaUVMZFkxS1plb1lrbjcwU0tpQlhtZ21FS3VqWmhJejluM25valk4SFRQSk0wDQoycXQvRHRydUs0M0h" +
					"IbmhsVTJQSklGek9VUkZoWkFLcFRWU0RGR2ZuRm1WamdCdHNMM3R1ZTBhVExKSnNTaWl0DQp6VDVLTHorSU41ZDFaY21rOEw3" +
					"M2tCTkw5YjBUdHNOZXpJN3Qvd3VkclhFampMTncyc3c3WFR3Yzl3OCs0ZEkxDQo4RnF5bXlBSw0KLS0tLS1FTkQgQ0VSVElGS" +
					"UNBVEUtLS0tLQ0K";

	/**
	 * @return Bytes of a PEM-encoded certificate.
	 */
	static byte[] certificatePemBytes() {
		return Base64.getDecoder().decode(CERTIFICATE_PEM_BASE64);
	}

	/**
	 * @param name
	 * 		Element name.
	 * @param attributes
	 * 		Element attributes.
	 *
	 * @return Manifest element with the given name and attributes.
	 */
	@Nonnull
	static ManifestElement manifestElement(@Nonnull String name, @Nonnull Map<String, String> attributes) {
		return new ManifestElement(name, attributes);
	}

	/**
	 * @param elements
	 * 		Elements to include in the manifest.
	 *
	 * @return Mocked {@code AndroidManifest.xml} file with the given elements.
	 */
	@Nonnull
	static BinaryXmlFileInfo mockManifest(ManifestElement... elements) {
		// Base mock,
		BinaryXmlFileInfo manifest = mock(BinaryXmlFileInfo.class);
		when(manifest.getName()).thenReturn("AndroidManifest.xml");
		when(manifest.getRawContent()).thenReturn(new byte[0]);

		// Configure the string pool and binary resource file to return the given elements.
		StringPoolChunk strings = mock(StringPoolChunk.class);
		when(strings.getString(anyInt())).thenAnswer(inv -> null);
		List<XmlStartElementChunk> startElements = new ArrayList<>();
		int index = 0;
		for (ManifestElement element : elements) {
			XmlStartElementChunk start = mock(XmlStartElementChunk.class);
			when(start.getName()).thenReturn(element.name());

			// Configure attributes for this element.
			List<XmlAttribute> attributes = new ArrayList<>();
			for (Map.Entry<String, String> entry : element.attributes().entrySet()) {
				int nameIndex = index++;
				int valueIndex = index++;
				XmlAttribute attribute = mock(XmlAttribute.class);
				when(attribute.nameIndex()).thenReturn(nameIndex);
				when(attribute.rawValueIndex()).thenReturn(valueIndex);
				when(strings.getString(nameIndex)).thenReturn(entry.getKey());
				when(strings.getString(valueIndex)).thenReturn(entry.getValue());
				attributes.add(attribute);
			}
			when(start.getAttributes()).thenReturn(attributes);
			startElements.add(start);
		}

		// Wire up remaining getters.
		BinaryResourceFile binary = mock(BinaryResourceFile.class);
		when(strings.getStringCount()).thenReturn(index);
		when(binary.getChunks()).thenReturn(List.copyOf(startElements));
		when(manifest.getChunkModel()).thenReturn(binary);
		when(manifest.getStringPoolChunk()).thenReturn(strings);
		return manifest;
	}

	/**
	 * Temp record to hold manifest element data for mocking.
	 *
	 * @param name
	 * 		Element name.
	 * @param attributes
	 * 		Element attributes.
	 */
	record ManifestElement(String name, Map<String, String> attributes) {}
}
