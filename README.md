# Anti VPN Service (AVS)
A powerful anti vpn service, using multiple types of providers, like downloaded address ranges or online services,
to protect your Mindustry server against players using VPNs (also useful against classic DDoS attacks). <br>
Custom whitelist and blacklist are given to allow or block specific addresses. <br>
And there are many settings to configure the plugin to your needs. 



---



### Requirements
**Java 14 or newer is required to run this plugin.** <br>
Mindustry server, v146 or newer.


### Commands
**Server commands:**
  * `avs-config [name] [value...]` Anti VPN Service's configuration
  * `anti-vpn [provider] [args...]` Providers' configuration of Anti VPN Service

**Admin commands:**
  * `/avs [help|arg0] [args...]` Configuration of some settings and providers for admins


### Releases
Prebuild relases can be found [here](https://github.com/Xpdustry/anti-vpn-service/releases)


### Building a Jar
Just run the command: `./gradlew build clean`, and the plugin will compile and export automatically.


### Installing
Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins by running the `mods` command.