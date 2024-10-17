# Anti VPN Service (AVS)
A powerful anti vpn service, using multiple types of providers, like downloaded address ranges or online services,
to protect your Mindustry server against players using VPNs (also useful against classic DDoS attacks).

Custom whitelist and blacklist are given to allow or block specific addresses.
And there are many settings, to configure the plugin to your needs. 



---



### Requirements
 * **Java 14 or newer is required to run this plugin.** <br>
 * Mindustry server, v146 or newer.


### Commands
**Server command:** `avs [command] [args...]` Anti VPN Service command line manager
**Admin command:** `/avs [command] [args...]` Restricted Anti VPN Service manager for admins


### Releases
Prebuild releases can be found [here](https://github.com/Xpdustry/anti-vpn-service/releases)


### Building a Jar
Just run the command: `./gradlew build clean`, and the plugin will compile and export automatically.


### Installing
Simply place the output jar from the step above, in your server's `config/mods` directory, and restart the server.
List your currently installed plugins by running the `mods` command.