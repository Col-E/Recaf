{
  pkgs,
  lib,
  version,
  buildGradleApplication,
  gradle,
  jdk
}:
buildGradleApplication {
  pname = "recaf";
  version = version;
  src = ./.;
  gradle = gradle;
  jdk = jdk;
  nativeBuildInputs = [
    pkgs.wrapGAppsHook
    pkgs.git
  ];
  repositories = ["https://plugins.gradle.org/m2/" "https://repo1.maven.org/maven2/" "https://maven.google.com" "https://maven.quiltmc.org/repository/release/" "https://jitpack.io"];
  buildTask = ":recaf-ui:installDist";
  installLocation = "recaf-ui/build/install/*/";
  meta = with lib; {
    description = "A Java Bytecode editor";
    longDescription = ''
      Recaf is a Java Bytecode editor
    '';
    sourceProvenance = with sourceTypes; [
      fromSource
      binaryBytecode
    ];
    platforms = platforms.unix;
  };
}
