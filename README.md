# GB ROM Tool

A basic GB(C) ROM tool written in Java.

Reads a header from a ROM and returns the following information:  
- Filename
- Title
- Manufacturer Code (if it exists)
- CGB Flag (if it exists)
- New Licensee Code (if it exists)
- SGB Flag
- Cartridge Type
- ROM Size
- RAM Size
- Destinate Code
- Old Licensee Code (if it exists)
- Mask ROM Version
- Header Checksum & Validity
- Global Checksum & Validity
- Boot Logo Validity

ROM information can then be saved to and loaded from a Derby/JavaDB database.  
Only part of the header information is saved - not full ROMs.  
This tool cannot be used to facilitate piracy.  

The database is created in the same directory as the program, and the Collection table (where ROM info is saved) persists between runs.

**Links to resources used:**  
- [Pan Docs - The Cartridge Header](https://gbdev.io/pandocs/The_Cartridge_Header.html)
- [FlashGBX](https://github.com/lesserkuma/FlashGBX/)
