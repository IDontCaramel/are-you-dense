# Are You Dense

Are You Dense is a server side fabric 1.20.1 mod for changing the density of ore spawns in minecraft.

(this mod does not adjust the placement of ore spawns it just places only some of the original amount)

## install

Download the [latest](https://github.com/IDontCaramel/are-you-dense/releases) version of the mod from releases
Add the mod jar and the fabric api to your servers mods folder.

The config file is `config/are-you-dense.toml`. It is optional and is only
created when an operator saves a change

```toml
[ores]
"minecraft:diamond_ore" = 0.5
"minecraft:deepslate_diamond_ore" = 0.5
```

Ore family members share one value. A value of `0` disables placement attempts;
values above `1` increase attempts and can make chunk generation slightly slower

## commands

Operators with permission level 2 can use:

- `/ayd set <block> <multiplier>`
- `/ayd reset <block>`
- `/ayd reset all`
- `/ayd list`


## License

This project is available under the [MIT License](LICENSE).
