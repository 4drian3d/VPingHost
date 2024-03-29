# VPingHost

[![Discord](https://img.shields.io/discord/899740810956910683?color=7289da&label=Discord)](https://discord.gg/5NMMzK5mAn)
[![Telegram](https://img.shields.io/badge/Telegram-Updates-229ED9)](https://t.me/Adrian3dUpdates)
[![Telegram](https://img.shields.io/badge/Telegram-Support-229ED9)](https://t.me/Adrian3dSupport)

A simple Velocity plugin that allows you to get information about other servers instantly.

This plugin was born as a test from this [Velocity pull request](https://github.com/PaperMC/Velocity/pull/1180),
but I thought to make it a public test plugin to check the proxy server or backend servers connectivity or MOTD response.

## Installation
- Download VPingHost from Modrinth
- Drag and drop on your plugins folder
- Start the server

## Features
- Send a ping to any Minecraft server and get its corresponding motd
- Specifies the version to ping with
- Specifies the version with which to ping the server

## Command
`/pinghost [host] (protocol version)`

Permission: `pinghost.command`

Example: `/pinghost my.server.net 765`

If you need to specify a specific port, you can add it with the syntax `host:port`.

### Differences between this plugin and VServerInfo
VServerInfo allows you to display information about your servers that you have in your own network.
VPingHost allows you to display information about the motd of any available server.
