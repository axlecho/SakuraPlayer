IjkPlayer简单封装
===

Usage
---

With Gradle:

*   Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```
allprojects {

    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
*   Step 2. Add the dependency
```
compile 'com.github.axlecho:SakuraPlayer:0.4'
compile 'tv.danmaku.ijk.media:ijkplayer-armv7a:0.8.4'
```

*	Step 3. Setup the playview

add PlayerView to layout
```xml
<com.axlecho.sakura.PlayerView
	android:id="@+id/player"
	android:layout_width="wrap_content"
	android:layout_height="wrap_content"
	android:text="Hello World!"
	app:layout_constraintBottom_toBottomOf="parent"
	app:layout_constraintLeft_toLeftOf="parent"
	app:layout_constraintRight_toRightOf="parent"
	app:layout_constraintTop_toTopOf="parent" />
```

setup at code
```java	
player = (PlayerView) findViewById(R.id.player);
player.setVideoUrl("https://www.bilibili.com/video/av15560010/");
```

License
---
```
Copyright (C) 2017 Axlecho

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```