package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.coley.recaf.ui.pane.table.TableDisplayMode;
import me.coley.recaf.ui.pane.table.TableWord;
import me.martinez.pe.PeImage;
import me.martinez.pe.headers.ImageOptionalHeader;

import java.util.Map;
import java.util.TreeMap;

/**
 * Table display for optional headers.
 *
 * @author Wolfie / win32kbase
 */
public class OptionalTableDisplayMode implements TableDisplayMode<PeImage> {
	/**
	 * @see <a href="https://docs.microsoft.com/en-us/windows/win32/debug/pe-format#dll-characteristics">
	 * PE Format - DLL Characteristics</a>
	 */
	private static final Map<Integer, String> DLL_CHARACTERISTICS_MAP = new TreeMap<>() {{
        put(0x0001, "IMAGE_DLLCHARACTERISTICS_RESERVED_MUST_BE_ZERO");
        put(0x0002, "IMAGE_DLLCHARACTERISTICS_RESERVED_MUST_BE_ZERO");
        put(0x0004, "IMAGE_DLLCHARACTERISTICS_RESERVED_MUST_BE_ZERO");
        put(0x0008, "IMAGE_DLLCHARACTERISTICS_RESERVED_MUST_BE_ZERO");
        put(0x0020, "IMAGE_DLLCHARACTERISTICS_HIGH_ENTROPY_VA");
        put(0x0040, "IMAGE_DLLCHARACTERISTICS_DYNAMIC_BASE");
        put(0x0080, "IMAGE_DLLCHARACTERISTICS_FORCE_INTEGRITY");
        put(0x0100, "IMAGE_DLLCHARACTERISTICS_NX_COMPAT");
        put(0x0200, "IMAGE_DLLCHARACTERISTICS_NO_ISOLATION");
        put(0x0400, "IMAGE_DLLCHARACTERISTICS_NO_SEH");
        put(0x0800, "IMAGE_DLLCHARACTERISTICS_NO_BIND");
        put(0x1000, "IMAGE_DLLCHARACTERISTICS_APPCONTAINER");
        put(0x2000, "IMAGE_DLLCHARACTERISTICS_WDM_DRIVER");
        put(0x4000, "IMAGE_DLLCHARACTERISTICS_GUARD_CF");
        put(0x8000, "IMAGE_DLLCHARACTERISTICS_TERMINAL_SERVER_AWARE");
    }};

	@Override
	public void apply(PeImage pe, SizedDataTypeTable table) {
		ImageOptionalHeader optionalHeader = pe.ntHeaders.optionalHeader;
		table.addWord("Magic", optionalHeader.magic, "Magic number");
		table.addByte("MajorLinkerVersion", optionalHeader.majorLinkerVersion, "Major linker version");
		table.addByte("MinorLinkerVersion", optionalHeader.minorLinkerVersion, "Minor linker version");
		table.addDword("SizeOfCode", (int) optionalHeader.sizeOfCode, "Size of code");
		table.addDword("SizeOfInitializedData", (int) optionalHeader.sizeOfInitializedData, "Size of initialized data");
		table.addDword("SizeOfUninitializedData", (int) optionalHeader.sizeOfUninitializedData, "Size of uninitialized data");
		table.addDword("AddressOfEntryPoint", (int) optionalHeader.addressOfEntryPoint, "Address of entry point (.unkn)");
		table.addDword("BaseOfCode", (int) optionalHeader.baseOfCode, "Base of code");
		table.addDword("BaseOfData", (int) optionalHeader.baseOfData, "Base of data");
		table.addAddress("ImageBase", optionalHeader.imageBase, "Image base", pe);
		table.addDword("SectionAlignment", (int) optionalHeader.sectionAlignment, "Section alignment");
		table.addDword("FileAlignment", (int) optionalHeader.fileAlignment, "File alignment");
		table.addWord("MajorOperatingSystemVersion", optionalHeader.majorOperatingSystemVersion, "Major operating system version");
		table.addWord("MinorOperatingSystemVersion", optionalHeader.minorOperatingSystemVersion, "Minor operating system version");
		table.addWord("MajorImageVersion", optionalHeader.majorImageVersion, "Major image version");
		table.addWord("MinorImageVersion", optionalHeader.minorImageVersion, "Minor image version");
		table.addWord("MajorSubsystemVersion", optionalHeader.majorSubsystemVersion, "Major subsystem version");
		table.addWord("MinorSubsystemVersion", optionalHeader.minorSubsystemVersion, "Minor subsystem version");
		table.addDword("Win32VersionValue", (int) optionalHeader.win32VersionValue, "Win32 version value");
		table.addDword("SizeOfImage", (int) optionalHeader.sizeOfImage, "Size of image");
		table.addDword("SizeOfHeaders", (int) optionalHeader.sizeOfHeaders, "Size of headers");
		table.addDword("CheckSum", (int) optionalHeader.checkSum, "Checksum");
		table.addWord("Subsystem", optionalHeader.subsystem, getSubsystem(optionalHeader.subsystem));
		table.addWord("DllCharacteristics", optionalHeader.dllCharacteristics, "DLL characteristics");

		int dllCharacteristics = optionalHeader.dllCharacteristics;
		DLL_CHARACTERISTICS_MAP.forEach((characteristicValue, name) -> {
			if ((dllCharacteristics & characteristicValue) > 0) {
				table.getItems().add(new TableWord("", characteristicValue, name));
			}
		});

		table.addAddress("SizeOfStackReserve", optionalHeader.sizeOfStackReserve, "Size of stack reserve", pe);
		table.addAddress("SizeOfStackCommit", optionalHeader.sizeOfStackCommit, "Size of stack commit", pe);
		table.addAddress("SizeOfHeapReserve", optionalHeader.sizeOfHeapReserve, "Size of heap reserve", pe);
		table.addAddress("SizeOfHeapCommit", optionalHeader.sizeOfHeapCommit, "Size of heap commit", pe);
		table.addDword("LoaderFlags", (int) optionalHeader.loaderFlags, "Loader flags");
		table.addDword("NumberOfRvaAndSizes", (int) optionalHeader.numberOfRvaAndSizes, "Number of RVA and sizes");
	}


	/**
	 * @param subsystem
	 * 		Subsystem type key.
	 *
	 * @return String representation of subsystem type.
	 *
	 * @see <a href="https://docs.microsoft.com/en-us/windows/win32/debug/pe-format#windows-subsystem">
	 * PE Format - Windows Subsystem</a>
	 */
	private static String getSubsystem(int subsystem) {
		switch (subsystem) {
			case 1:
				return "Device drivers and native Windows processes";
			case 2:
				return "The Windows graphical user interface (GUI) subsystem";
			case 3:
				return "The Windows character subsystem";
			case 5:
				return "The OS/2 character subsystem";
			case 7:
				return "The Posix character subsystem";
			case 8:
				return "Native Win9x driver";
			case 9:
				return "Windows CE";
			case 10:
				return "An Extensible Firmware Interface (EFI) application";
			case 11:
				return "An EFI driver with boot services";
			case 12:
				return "An EFI driver with run-time services";
			case 13:
				return "An EFI ROM image";
			case 14:
				return "XBOX";
			case 16:
				return "Windows boot application";
			default:
				return "An unknown subsystem";
		}
	}
}
