# SB Jenkins Dynamic Plugin

This is a plugin for Jenkins-CI (http://jenkins-ci.org) which allows defining
build parameters, which default values are generated dynamically by a script.
Currently, only Groovy is supported as a script language. The script is executed
each time before the build parameters are shown to the user. It can be executed
either on the master or on a slave, if one is assigned for the build. This
behavior can be configured for each parameter individually.

The plugin provides two types of parameters: simple text-filed parameter and a
choice (drop-down) parameter. If the parameter is a text-field, the
corresponding script must return a single value. If the parameter is a choice
parameter, then the script must resturn a list of values.

Groovy scripts for a parameter value can be defined by using the Jenkins Scriptler plugin or in the 
parameter configuration inline.

## How to build?

Since, this is a standard Maven Jenkins plugin project, the plugin can be build
by running

    $ mvn install

which will create the installable plugin archive target/dynamicparameter.hpi.

## How to test and run?

The jenkins plugin can be run by the maven goal:

    $ mvn hpi:run

## Authors

- Dimitar Popov <dimitar.popov@seitenbau.com>
- Christian Baranowski

## License

Apache License, Version 2.0 (current)
http://www.apache.org/licenses/LICENSE-2.0