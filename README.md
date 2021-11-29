# C Klib

CKlib is a gradle plugin that will build and package C/C++/Objective-C code for Kotlin/Native.

# Note

The main Kotlin project has changed how locally embedded C-like code is included in libraries, so we'll probably remove
this project from public access soon until we land on a complete answer to handling this (our current solution is kind 
of a copy/paste of the main Kotlin solution).

You can use this, but we won't be supporting it publicly as it's kind of brittle to set up and debug. Just FYI.

License
=======

    Copyright 2021 Touchlab, Inc.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.