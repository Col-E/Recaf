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
    pkgs.autoPatchelfHook
    pkgs.wrapGAppsHook
    pkgs.git
  ];
  repositories = ["https://plugins.gradle.org/m2/" "https://repo1.maven.org/maven2/" "https://maven.google.com" "https://maven.quiltmc.org/repository/release/" "https://jitpack.io"];
  buildTask = "build";
  meta = with lib; {
    description = "Spring Boot Example Application";
    longDescription = ''
      Will start a server at Port 8080
    '';
    sourceProvenance = with sourceTypes; [
      fromSource
      binaryBytecode
    ];
    platforms = platforms.unix;
  };
}
