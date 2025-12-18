# CHelper plugin for IntelliJ IDEA

A maintained fork of the CHelper plugin, updated for modern versions of IntelliJ IDEA (2025.x and above).


## Introduction

CHelper is an IntelliJ IDEA plugin for competitive programming in Java.

Key features:

- **Sample Test Case Parsing**: Integrate with the [Competitive Companion browser extension](https://github.com/jmerle/competitive-companion) to download sample test data from various competitive programming platforms.
- **Local Testing**: Check your solution against saved test cases.
- **Code Inlining**: Bundle your custom library classes into a single file for submission.
- **Stress Testing**: Run your solution against a test generator and a reference solution to identify failing cases.


## Installation

- Download the [CHelper plugin zip file from the latest release](https://github.com/nathanwn/idea-chelper/releases).
- In IntelliJ, `Settings > Install Plugin from Disk`, then select the zip file.
- Restart IntelliJ.

There is not yet a plan to publish this fork to the JetBrains Marketplace.


## About this fork

This fork is a continuation of the original [CHelper plugin by Egor Kulikov](https://github.com/EgorKulikov/idea-chelper). Unfortunately, the maintenance of the original plugin has been largely inactive since 2018. This fork builds upon the work of [algobot76](https://github.com/algobot76/idea-chelperx), who did a good job migrating the project to Gradle.

The goal of this fork is to keep the plugin compatible with the most recent IntelliJ IDEA releases (2025+) and fix bugs that arise from IntelliJ Platform API changes.

This fork follows a minimalist approach. Stability of the most-used features and ease of maintenance are prioritized, by removing legacy features that are no longer widely used or have better alternatives. Here are some differences compared to the original version of CHelper:

- Problem and contest parsers integrating with the legacy [CHelper browser extension](https://chromewebstore.google.com/detail/chelper-extension/eicjndbmlajfjdhephbcjdeegmmoadip) have been removed in favor of Competitive Companion integration.
- TopCoder support has been dropped following [TopCoder shutting down Arena applet](https://codeforces.com/blog/entry/131881).
- Integer overflow detection using the Cojac library has been dropped due to repeated compatibility issues with the new versions of Java.


## Acknowledgement

- Egor Kulikov, who was the original author of the CHelper plugin.
- algobot76, who did the hard work of moving the project to Gradle, which made this version possible.
- Petr Mitrichev, who showcased the plugin in his YouTube screencasts.
- Other people who has reported issues and submit fixes over the years.
