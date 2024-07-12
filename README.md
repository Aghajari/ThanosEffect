# ThanosEffect

This repository contains two modules, **ThanosEffect-Canvas** and **ThanosEffect-OpenGL**, which implement the ThanosEffect using both **Canvas** and **OpenGL** techniques. The ThanosEffect refers to a visual effect inspired by the disintegration effect seen in popular media, where an image appears to break into particles and gradually disperse.

This implementation was created for Telegram Android UI contest. The implementation on the Telegram source can be found [here](https://github.com/Aghajari/Telegram-DustEffect-Implementation).

<img src='./preview.gif' width=200/>

## ThanosEffect-Canvas
This module implements the ThanosEffect using the Android Canvas API. While this approach is straightforward and easy to implement, it can suffer from performance issues when handling a large number of particles.

## ThanosEffect-OpenGL
This module implements the ThanosEffect using OpenGL ES, which provides better performance for rendering complex animations compared to Canvas.
