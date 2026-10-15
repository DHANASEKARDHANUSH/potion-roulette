Potion Roulette — Installation Guide

Download: Replace this line with the published CurseForge project link once the mod is approved and live.

Potion Roulette is a Fabric mod for Minecraft: Java Edition 26.3. Start the challenge with /potionroulettestart, receive a random beneficial or harmful potion effect immediately, and then receive another effect every 60 seconds. Effects are configured to have infinite duration. Obtain an additional diamond during the challenge to win.

Requirements

Minecraft: Java Edition 26.3

Fabric Loader compatible with Minecraft 26.3

Fabric API for Minecraft 26.3

The Potion Roulette .jar file from the official project download page

Do not use a Forge or NeoForge profile. The mod is built for Fabric.

Install on Windows (official Minecraft Launcher)

Install Fabric Loader. Download the installer from fabricmc.net/use/installer. Select Client, choose Minecraft 26.3, and install the compatible Fabric profile.

Launch Fabric once. Open the Minecraft Launcher, select the new Fabric 26.3 profile, and launch the game. Close Minecraft afterwards.

Download the required files. From the official Potion Roulette project page, download the Potion Roulette .jar. Also download Fabric API for Minecraft 26.3 from a trusted source such as Modrinth's Fabric API page or CurseForge. Choose a Fabric API file that explicitly supports Minecraft 26.3.

Open the mods folder. Press Win + R, enter %appdata%\.minecraft\mods, and press Enter. If the mods folder does not exist, create it.

Copy the JAR files. Put the Potion Roulette .jar and the compatible Fabric API .jar in the mods folder. Do not extract or unzip either JAR.

Launch the game. In the Minecraft Launcher, select the Fabric 26.3 profile and press Play.

Start the challenge. Enter a Survival world, open chat with T, and run:

/potionroulettestart

You should see the on-screen challenge title and receive your first random effect immediately. Another effect is selected every 60 seconds. Obtaining an additional diamond during the active challenge triggers the victory message.

If you use a custom game directory

The mods folder must be inside the game directory configured for that Minecraft Launcher installation, not necessarily %appdata%\.minecraft.

Install on macOS or Linux

Install Fabric Loader for Minecraft 26.3, then place the Potion Roulette .jar and a compatible Fabric API .jar in the Minecraft mods folder:

macOS: ~/Library/Application Support/minecraft/mods

Linux: ~/.minecraft/mods

Launch the Fabric 26.3 profile and use /potionroulettestart in a Survival world.

Multiplayer

For a dedicated multiplayer server, the server owner must install Fabric Loader for Minecraft 26.3, Fabric API for 26.3, and the Potion Roulette mod on the server. Back up the world before adding mods. All players must join a compatible Minecraft version. If the mod page later specifies additional client-side requirements, follow those instructions too.

Troubleshooting

The game says a dependency is missing: Install Fabric API for Minecraft 26.3.

The game crashes on launch: Check that the Minecraft version is 26.3, the profile uses Fabric (not Forge/NeoForge), and both JAR files support that version.

The mod does not appear to load: Make sure you copied the .jar file itself—not the ZIP source code, a -sources JAR, or a development JAR—into the correct mods folder.

The command is unknown: Confirm the game was launched with the Fabric 26.3 profile and that the mod loaded successfully. Check latest.log for missing-dependency or mod-loading errors.

Download the mod only from the official project page. Back up important worlds before testing mods.