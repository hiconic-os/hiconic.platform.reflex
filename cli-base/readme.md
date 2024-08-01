# Why does this exist?

This was originally part of cli-rx-module, but some parts need to be shared with the shell completion script generator.

But depending cli-rx-module (from the module with the generator) is problematic, because that automatically shuts down the JVM.

Hence, the common functionality was extracted here.