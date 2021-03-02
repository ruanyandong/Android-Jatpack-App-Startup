# Android-Jatpack-App-Startup
# 简介

App Startup`提供了一种在应用程序启动时初始化组件的简单、高效的方法。库开发人员和应用程序开发人员都可以使用应用程序启动来简化启动序列并显式设置初始化顺序。
`App Startup`允许您定义共享单个内容提供程序的组件初始化程序，而不是为每个需要初始化的组件定义单独的内容提供程序。这可以显著缩短应用程序启动时间。
# App Startup解决什么问题
在学习`App Startup`的用法之前，首先我们需要搞清楚的是，`App Startup`具体是用来解决什么问题的。`App Startup`是一个可以用于加速App启动速度的一个库。

我们经常需要在app启动的时候初始化第三方库，给第三方对象提供一个Context对象，我们经常会这么写：

```kotlin
/**
 * 常规方式 在应用启动时初始化需要Context的类库，但如果（用于讲解App Startup使用）
 */
public class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        library.init(this)
    }
}
```
如果项目中在启动时需要初始化许多第三方库，那么会有以下写法：

```kotlin
class MyApp : Application() {

   override fun onCreate() {
        super.onCreate()
       library.init(this)
       libraryA.init(this)
       libraryB.init(this)
       libraryC.init(this)
       ...
   }
 
}
```
随着项目越来越大，Application里会变得越来越臃肿。

有没有巧妙的办法来避免显示地调用初始化接口，而是可以自动调用初始化接口，这种办法就是借助`ContentProvider`。

`ContentProvider`我们都知道是Android四大组件之一，它的主要作用是跨应用程序共享数据。比如为什么我们可以读取到电话簿中的联系人、相册中的照片等数据，借助的都是`ContentProvider`。

然而今天我们并没有打算使用ContentProvider来跨应用程序共享数据，只是准备使用它进行初始化而已。我们来看如下代码：

```kotlin
public final class MyProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            library.init(this);
        } else {
            throw new StartupException("Context cannot be null");
        }
        return true;
    }

    @Nullable
    @Override
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        throw new IllegalStateException("Not allowed.");
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new IllegalStateException("Not allowed.");
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new IllegalStateException("Not allowed.");
    }

    @Override
    public int delete(
            @NonNull Uri uri,
            @Nullable String selection,
            @Nullable String[] selectionArgs) {
        throw new IllegalStateException("Not allowed.");
    }

    @Override
    public int update(
            @NonNull Uri uri,
            @Nullable ContentValues values,
            @Nullable String selection,
            @Nullable String[] selectionArgs) {
        throw new IllegalStateException("Not allowed.");
    }
}

```
这里定义了一个MyProvider，并让它继承自ContentProvider，然后我们在onCreate()方法中调用了第三方库library的初始化接口。注意在ContentProvider中也是可以获取到Context的。

继承了ContentProvider之后，要重写很多个方法的，只不过其他方法在我们这个场景下完全使用不到，所以你可以在那些方法中直接抛出一个异常，或者进行空实现都是可以的。

四大组件是需要在AndroidManifest.xml文件中进行注册才可以使用的，因此记得添加如下内容：


```xml
<application>
   <provider
       android:name=".MyProvider"
       android:authorities="${applicationId}.myProvider"
       android:exported="false" />
</application>
```

那么，自定义的这个MyProvider它会在什么时候执行呢？我们来看一下这张流程图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210302225020970.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM0NjgxNTgw,size_16,color_FFFFFF,t_70)
可以看到，一个应用程序的执行顺序是这个样子的。首先调用Application的attachBaseContext()方法，然后调用ContentProvider的onCreate()方法，接下来调用Application的onCreate()方法。

那么，假如第三方库在自己的库当中实现了上述的MyProvider，会发生什么情况呢？

你会发现第三方库的init方法不用在Application的onCreate()方法中初始化了，因为在MyProvider当中这个方法会被自动调用，这样在进入Application的onCreate()方法时，第三方库其实已经初始化过了。

有没有觉得这种设计方式很巧妙？它可以将库的用法进一步简化，不需要你主动去调用初始化接口，而是将这个工作在背后悄悄自动完成了。

那么有哪些库使用了这种设计方式呢？这个真的有很多了，比如说Facebook的库，Firebase的库，还有我们所熟知的WorkManager，Lifecycles等等。

看上去如此巧妙的技术方案，那么它有没有什么缺点呢？

有，缺点就是，ContentProvider会增加许多额外的耗时。

毕竟ContentProvider是Android四大组件之一，这个组件相对来说是比较重量级的。也就是说，本来我的初始化操作可能是一个非常轻量级的操作，依赖于ContentProvider之后就变成了一个重量级的操作了。

关于ContentProvider的耗时，Google官方也有给出一个测试结果：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021030222533989.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM0NjgxNTgw,size_16,color_FFFFFF,t_70)
这是在一台搭载Android 10系统的Pixel2手机上测试的情况。可以看到，一个空的ContentProvider大约会占用2ms的耗时，随着ContentProvider的增加，耗时也会跟着一起增加。如果你的应用程序中使用了50个ContentProvider，那么将会占用接近20ms的耗时。

注意这还只是空ContentProvider的耗时，并没有算上你在ContentProvider中执行逻辑的耗时。

这个测试结果告诉我们，虽然刚才所介绍的使用ContentProvider来进行初始化的设计方式很巧妙，但是如果每个第三方库都自己创建了一个ContentProvider，那么最终我们App的启动速度就会受到比较大的影响。有没有办法解决这个问题呢？

就是使用我们今天要介绍的主题：`App Startup`。

那么App Startup是如何解决这个问题的呢？它可以将所有用于初始化的ContentProvider合并成一个，从而使App的启动速度变得更快。

具体来讲，App Startup内部也创建了一个ContentProvider，并提供了一套用于初始化的标准。然后对于其他第三方库来说，就不需要再自己创建ContentProvider了，都按这套标准进行实现就行了，可以保证第三方库在App启动之前都成功进行初始化。

了解了App Startup具体是用来解决什么问题的，以及它的实现原理，接下来我们开始学习它的用法，这部分就非常简单了。

# App Startup使用
要在库或应用程序中使用Jetpack App Startup，首先请将以下内容添加到Gradle文件：

```groovy
dependencies {
    implementation "androidx.startup:startup-runtime:1.0.0"
}
```
接下来要定义一个用于执行初始化的Initializer，并实现App Startup库的Initializer接口，如下所示：

```kotlin
class MyInitializer : Initializer<Unit>{
    override fun create(context: Context) {
        library.init(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }

}
```
实现Initializer接口要求重现两个方法，在create()方法中进行初始化操作就可以了，create()方法会把我们需要的Context参数传递进来。

dependencies()方法表示，当前的MyInitializer是否还依赖于其他的Initializer，如果有的话，就在这里进行配置，App Startup会保证先初始化依赖的Initializer，然后才会初始化当前的MyInitializer。

当然，绝大多数的情况下，我们的初始化操作都是不会依赖于其他Initializer的，所以通常直接返回一个emptyList()就可以了

定义好了Initializer之后，，将它配置到AndroidManifest.xml当中。但是注意，这里的配置是有比较严格的格式要求的，如下所示：

```xml
<application >
	<provider
		android:name="androidx.startup.InitializationProvider"
		android:authorities="${applicationId}.androidx-startup"
		android:exported="false"
		tools:node="merge">
		<meta-data
			android:name="com.example.MyInitializer"
			android:value="androidx.startup" />
	</provider>

</application>
```
上述配置，我们能修改的地方并不多，只有meta-data中的android:name部分我们需要指定成我们自定义的Initializer的全路径类名，其他部分都是不能修改的，否则App Startup库可能会无法正常工作。

App Startup库的用法就是这么简单，基本我将它总结成了三步走的操作。

 1. 引入App Startup的库。
 2. 自定义一个用于初始化的Initializer。
 3. 将自定义Initializer配置到AndroidManifest.xml当中。

这样，当App启动的时候会自动执行App Startup库中内置的ContentProvider，并在它的ContentProvider中会搜寻所有注册的Initializer，然后逐个调用它们的create()方法来进行初始化操作。

# 延迟（手动）初始化个别库
现在我们已经知道，所有的Initializer都会在App启动的时候自动执行初始化操作。但是如果不希望第三方库在启动的时候自动初始化，而是想要在特定的时机手动初始化，这要怎么办呢？

首先，找到第三方库用于初始化的Initializer的全路径类名是什么，比如上述例子当中的com.example.MyInitializer。

然后，在你的项目的AndroidManifest.xml当中加入如下配置：

```xml
<application >
	<provider
		android:name="androidx.startup.InitializationProvider"
		android:authorities="${applicationId}.androidx-startup"
		android:exported="false"
		tools:node="merge">
		<meta-data
			android:name="com.example.LitePalInitializer"
			tools:node="remove" />
	</provider>
</application>
```
区别就在于，这里在MyInitializer的meta-data当中加入了一个tools:node="remove"的标记。

这个标记用于告诉manifest merger tool，在最后打包成APK时，将所有android:name是com.example.MyInitializer的meta-data节点全部删除。

这样，第三方库在自己的AndroidManifest.xml中配置的Initializer也会被删除，既然删除了，App Startup在启动的时候肯定就无法初始化它了。

之后手动去初始化第三方库的代码如下所示：

```kotlin
AppInitializer.getInstance(this)
    .initializeComponent(MyInitializer::class.java)
```
将MyInitializer传入到initializeComponent()方法当中即可，App Startup库会按照同样的标准去调用其create()方法来执行初始化操作。
# 延迟（手动）初始化所有库

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="remove" />
```
之后手动去初始化第三方库的代码如下所示：

```kotlin
AppInitializer.getInstance(this)
    .initializeComponent(MyInitializer::class.java)
```


App Startup的功能基本就全部讲解完了。

如果你是一个库开发者，并且使用了ContentProvider的方式来进行初始化操作，那么你应该接入App Startup，这样可以让接入你的库的App降低启动耗时。而如果你是一个App开发者，我认为使用ContentProvider来进行初始化操作的概率很低，所以可能App Startup对你来说用处并不大。

当然，考虑到业务逻辑分离的代码结构，App的开发者也可以考虑将一些原来放在Application中的初始化代码，移动到一个Initializer中去单独执行，或许可以让你的代码结构变得更加合理与清晰。



参考文章

[App Startup Android官方文档](https://developer.android.com/topic/libraries/app-startup#java)

[Jetpack新成员，App Startup一篇就懂](https://guolin.blog.csdn.net/article/details/108026357)









