package me.coley.recaf.ui.panel.pe;

import com.kichik.pecoff4j.OptionalHeader;
import com.kichik.pecoff4j.PE;

import java.util.Map;
import java.util.TreeMap;

/**
 * Table display for optional headers.
 *
 * @author Wolfie / win32kbase
 */
public class OptionalTableDisplayMode implements TableDisplayMode {
	/**
	 * @see <a href="https://docs.microsoft.com/en-us/windows/win32/debug/pe-format#dll-characteristics">
	 * PE Format - DLL Characteristics</a>
	 */
	private static final Map<Integer, String> DLL_CHARACTERISTICS_MAP = new TreeMap<Integer, String>() {{
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
	public void apply(PE pe, SizedDataTypeTable table) {
		OptionalHeader optionalHeader = pe.getOptionalHeader();
		table.addWord("Magic", optionalHeader.getMagic(), "Magic number");
		table.addByte("MajorLinkerVersion", optionalHeader.getMajorLinkerVersion(), "Major linker version");
		table.addByte("MinorLinkerVersion", optionalHeader.getMinorLinkerVersion(), "Minor linker version");
		table.addDword("SizeOfCode", optionalHeader.getSizeOfCode(), "Size of code");
		table.addDword("SizeOfInitializedData", optionalHeader.getSizeOfInitializedData(), "Size of initialized data");
		table.addDword("SizeOfUninitializedData", optionalHeader.getSizeOfUninitializedData(), "Size of uninitialized data");
		table.addDword("AddressOfEntryPoint", optionalHeader.getAddressOfEntryPoint(), "Address of entry point (.unkn)");
		table.addDword("BaseOfCode", optionalHeader.getBaseOfCode(), "Base of code");
		table.addDword("BaseOfData", optionalHeader.getBaseOfData(), "Base of data");
		table.addAddress("ImageBase", optionalHeader.getImageBase(), "Image base", pe);
		table.addDword("SectionAlignment", optionalHeader.getSectionAlignment(), "Section alignment");
		table.addDword("FileAlignment", optionalHeader.getFileAlignment(), "File alignment");
		table.addWord("MajorOperatingSystemVersion", optionalHeader.getMajorOperatingSystemVersion(), "Major operating system version");
		table.addWord("MinorOperatingSystemVersion", optionalHeader.getMinorOperatingSystemVersion(), "Minor operating system version");
		table.addWord("MajorImageVersion", optionalHeader.getMajorImageVersion(), "Major image version");
		table.addWord("MinorImageVersion", optionalHeader.getMinorImageVersion(), "Minor image version");
		table.addWord("MajorSubsystemVersion", optionalHeader.getMajorSubsystemVersion(), "Major subsystem version");
		table.addWord("MinorSubsystemVersion", optionalHeader.getMinorSubsystemVersion(), "Minor subsystem version");
		table.addDword("Win32VersionValue", optionalHeader.getWin32VersionValue(), "Win32 version value");
		table.addDword("SizeOfImage", optionalHeader.getSizeOfImage(), "Size of image");
		table.addDword("SizeOfHeaders", optionalHeader.getSizeOfHeaders(), "Size of headers");
		table.addDword("CheckSum", optionalHeader.getCheckSum(), "Checksum");
		table.addWord("Subsystem", optionalHeader.getSubsystem(), getSubsystem(optionalHeader.getSubsystem()));
		table.addWord("DllCharacteristics", optionalHeader.getDllCharacteristics(), "DLL characteristics");

		int dllCharacteristics = optionalHeader.getDllCharacteristics();
		DLL_CHARACTERISTICS_MAP.forEach((characteristicValue, name) -> {
			if ((dllCharacteristics & characteristicValue) > 0) {
				table.getItems().add(new TableWord("", characteristicValue, name));
			}
		});

		table.addAddress("SizeOfStackReserve", optionalHeader.getSizeOfStackReserve(), "Size of stack reserve", pe);
		table.addAddress("SizeOfStackCommit", optionalHeader.getSizeOfStackCommit(), "Size of stack commit", pe);
		table.addAddress("SizeOfHeapReserve", optionalHeader.getSizeOfHeapReserve(), "Size of heap reserve", pe);
		table.addAddress("SizeOfHeapCommit", optionalHeader.getSizeOfHeapCommit(), "Size of heap commit", pe);
		table.addDword("LoaderFlags", optionalHeader.getLoaderFlags(), "Loader flags");
		table.addDword("NumberOfRvaAndSizes", optionalHeader.getNumberOfRvaAndSizes(), "Number of RVA and sizes");
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
