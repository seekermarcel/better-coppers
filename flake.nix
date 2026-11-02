{
  description = "Better Coppers development shell";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { nixpkgs, ... }:
    let
      forAllSystems = nixpkgs.lib.genAttrs [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
    in
    {
      devShells = forAllSystems (system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          # Libraries the game loads at runtime for runClient: graphics, windowing, input and sound.
          clientLibs = with pkgs; [
            alsa-lib
            flite
            libdecor
            libGL
            libpulseaudio
            libx11
            libxcursor
            libxext
            libxfixes
            libxi
            libxkbcommon
            libxrandr
            libxscrnsaver
            pipewire
            udev
            vulkan-loader
            wayland
          ];
        in {
          default = pkgs.mkShell ({
            packages = [ pkgs.jdk25 ];
          } // pkgs.lib.optionalAttrs pkgs.stdenv.isLinux {
            LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath clientLibs;
          });
        });
    };
}
