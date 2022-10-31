with (import <nixpkgs> {});

mkShell {
  buildInputs = [
    k6
    jdk17_headless
    sbt
    curl
    unzip
  ];
  shellHook = ''
  '';
}
