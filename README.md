# KeK-Language Compiler

## Description

KeK is a hobbyist project about create a own language based on LLVM.

## Example

```kek
func fibonacci(n: Integer) -> Integer {
    if (n == 0) {
        0
    }
    else if (n == 1) {
        1
    }
    else {
        fibonacci(n - 1) + fibonacci(n - 2)
    }
}

func main() -> Integer {
    fibonacci(5)
}
```

# Supported operating systems

| OS  | CPU Architecture | Flag |
| --- | ---------------- | ---- |
| [GunwOS](https://github.com/bronexproduction/GunwOS) | [x86](https://en.wikipedia.org/wiki/X86) | `--target-triple x86-pc-gunwos` |
| [Linux](https://github.com/torvalds/linux) | [x86-64](https://en.wikipedia.org/wiki/X86-64) | `--target-triple x86-pc-linux` |

# References

* [Grammar Summary](grammar.md)

# License

This project is licensed under the [Beerware](https://en.wikipedia.org/wiki/Beerware) license

# Contributors

* Piotr Merski ([merskip](https://github.com/merskip)) <[merskip@gmail.com](mailto:merskip@gmail.com)>