# Deyaml - A YAML Object Serialiser

As it currently stands, this project is lightweight, simple and barebones, this is a conscious
design choice as most use-cases don't require the complexities of more bloated tools.
This project will serve a simple purpose and offer a simple and reliable solution, no questions asked.

# Usage

- Serialisation must be performed on a data class, or class of similar form (with an open/empty constructor consisting of all vals/vars).
- It may work on other objects such as collections or classes with different forms, however these are not yet officially supported.
- Support for generic types is limited.

example of objects that do work: 
```kt
data class Config(val version: Int, val names: List<String>)
// OR
class Config(val version: Int, val names: List<String>)
// etc. 
```

Maps, nested or not can only use Int or String keys unless you register a converter 
