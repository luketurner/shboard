# shboard

*__NOTICE:__ This is a beta release. It's functional, but still under heavy development and subject to change without warning.*

shboard displays your EC2 instances in a ncurses-style terminal dashboard. It looks cool, and opens faster than the AWS console. New Relic Servers stats are also displayed if an API key is provided.

## Features

* Displays all EC2s within a given region, sorted and color-coded by name.
* Information available per server:
  * ID
  * Name
  * Public and private IPs and DNS names
  * Tags (visible on mouseover)
  * CPU usage (%)
  * Disk usage (%)
  * Memory usage (bytes)
  * Network I/O (bytes/sec)
* Click on a server to select it. (Or use arrow-keys to select.)
* Click on a selected server to open it in the AWS dashboard.
* Click on a public/private IP, or the instance ID, to copy the value to your clipboard.

## Installation and Usage

shboard is distributed as an npm package. Installation: `npm install -g shboard`

Usage: Run `shboard --aws-profile PROFILE --aws-region REGION` to launch the dashboard.

In order to avoid passing these arguments every time, you can do `shboard --aws-profile PROFILE --aws-region REGION --save-config` to write your settings to a config file on disk. Then `shboard` will use the specified profile/region automatically.

Use `shboard --help` for more information. The output is shown below for reference. Note that some of these parameters are not used for existing functionality (e.g. `--papertrail-key`).

```
Usage: shboard [options]

Options:
  -h, --help                                                      Display this help message and exit.
      --debug                                                     Enable debugging instrumentation (may impact performance).
      --run-tests                                                 Run test.check suite instead of launching shboard.
      --print-config                                              Print config (including API keys) to STDOUT and exit.
  -S, --save-config                                               Write current config (except API keys) to config file.
      --save-api-keys                                             Write API keys to config file. Implies --save-config.
  -M, --no-metrics                                                Disable New Relic metrics.
  -l, --log-file FILE                                             Output diagnostic logging into specified log file.
  -c, --config-file FILE     $XDG_CONFIG_HOME/shboard/config.edn  Override config file location
  -P, --aws-profile PROFILE  $AWS_PROFILE                         AWS profile to use. If none is specified, will use "default"
  -r, --aws-region REGION    $AWS_REGION                          AWS region to use. Overrides profile default.
      --new-relic-key KEY    $NEW_RELIC_API_KEY                   New Relic API key to use. Required for server metrics.
      --papertrail-key KEY   $PAPERTRAIL_API_KEY                  API key for Papertrail. Required for viewing logs.
```

### Configuration File

shboard is configurable via command-line arguments as well as with a `config.edn` file. [edn](https://github.com/edn-format/edn) is a Clojure-related data format that allows you to represent data more compactly than with JSON. By default, this file is looked for in `~/.config/shboard/config.edn`. You can view the configuration being used with: `shboard --print-config`, or automatically update the `config.edn` file with the running configuration using `shboard --save-config`.


## Building From Source

shboard is written in ClojureScript, which has to be compiled to Javascript before it can be executed with Node. This process is managed by [Boot](http://boot-clj.com/). See the [boot install instructions](https://github.com/boot-clj/boot#install) for details on how to get `boot` on your machine.

Once you have `boot`, the root directory of the project, run `boot build` to compile the application to the `target/` output directory. You should then be able to `node target/main.js` to run the dashboard.

There is also a dev build: `boot build-dev`, and a file watcher that rebuilds on changes: `boot follow`. Because the dev builds are compiled without optimizations, the application must be run from within the `target` directory: `cd target && node main.js`. However, the dev build is faster.

### NPM Dependencies

Node package dependencies can be managed with `boot` if they are in the CLJSJS repository. If not, use the `package.json` in the root folder to manage them. The modules are copied to the `target` directory as part of the build process.

## License

Copyright © 2017 Luke Turner

Distributed under the MIT License (SPDX:MIT)