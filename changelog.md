### 1.2:
 - changed files structure
 - added L10N bundle standard
 - changed loading system
 - improved address validator
 - optimized http classes
 - optimized cloud downloaded providers
 - added a move of the mindsutry DOS list to the custom blacklist
 - remade settings system 
 - improved online service providers
 - added configurations
 - added commands
 - added events to track every plugin verification steps
 - made the plugin api usable for other plugins
 - improved plugin loading
 - added an auto refresh
 - added a configuration file for each provider
 - fixed bug with random online provider when it's defined as not trusted
 - fixed others minor bugs
 - added more providers
 - changed waiting list for a cooldown system using cron expressions
 - added a verification of provider properties on loading
 - added a description for each providers
 - added option to force debug log level only for avs

### 1.0:
 - first release

--- 

### TODO: 
 - ~~make a rule to whitelist all ips of specific a UUID~~ (will be done in [Better-Security](https://github.com/xpdustry/Better-Security/))
 - make metrics *(probably another plugin)*
 - make a provider type that load addresses ranges from a file
 - make providers creatable with a config file
 - **PRIORITY:** make a documentation
 - add a feature to move the ``subnet-ban`` blacklist to the ``Blacklist`` provider.
