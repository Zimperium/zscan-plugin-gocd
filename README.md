# GoCD zScan Upload Plugin

## Overview

Welcome to the GoCD zScan Upload Plugin repository. This plugin is designed to help you integrate with zScan seamlessly. You can download, use, and modify this plugin under the MIT license.

## Getting Started

Please refer to [GoCD Plugin User Guide](https://docs.gocd.org/current/extension_points/plugin_user_guide.html) for more information on using plugins.

### Build Prerequisites

- This plugin requires Java 17
- The build systems uses Gradle.
- The plugin uses [Google GSON](https://github.com/google/gson) and [Stipe OkHttp](https://square.github.io/okhttp/) libraries

In your console, head over to the Authorizations tab in the Account Management section and generate a new API key. At a minimum, the following permissions are required:

- Common: Teams - Manage
- zScan: zScan Apps - Manage, zScan Assessments - View, zScan Builds - Upload

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/Zimperium/zscan-plugin-gocd
   cd zscan-plugin-gocd
   ```

2. Run Gradle:

   ```bash
   ./gradlew build
   ```

The build command's output is a jar file with the plugin code and resources.  It can be found in the build/libs folder.  
The name of the file is zscan-upload-plugin-(build-version).jar, e.g., zscan-upload-plugin-0.1.10.jar.

### Usage

1. The plugin needs to be copied into the plugins/external folder of your GoCD installation.  Access to the server machine is required.
   For more information, please refer to [GoCD Documentation](https://docs.gocd.org/current/extension_points/plugin_user_guide.html).
2. The following configuration options are available:
   - Endpoint (required): Base URL of Zimperium Console for your account, e.g., `https://ziap.zimperium.com`.
   - Client ID (required): Client ID part of the API Key created through the Authorization tab of your console
   - Client Secret (required): Client Secret part of the API Key.

   We recommend using environment variables configured for the appropriate stage to keep credentials.
   Secure variables should be used for the Client Secret part.

   - Team Name (optional): Team name to assign applications to. If no team name is provided or if a team with the provided name is not found,
     the 'Default' team is used. This setting is only relevant when uploading an application for the first time.
     To change the application's team, please use the zScan Console.
   - File(s) to Upload (Required): Pattern of files to upload.  Wildcards are allowed.  
     To prevent accidental flooding of zScan servers, only the first 5 matches will be processed.
   - Report Format: Specifies the format for the assessment report. For more information on SARIF, please see [OASIS Open](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html).

## License

This plugin is licensed under the MIT License. By using this plugin, you agree to the following terms:

```text
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Enhancements

Submitting improvements to the plugin is welcomed and all pull requests will be approved by Zimperium after review

## Support

If you have any questions or need assistance, please contact our support team at [support.zimperium.com] or visit our [support page](support.zimperium.com).
