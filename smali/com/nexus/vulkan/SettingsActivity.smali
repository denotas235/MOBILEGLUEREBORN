.class public Lcom/nexus/vulkan/SettingsActivity;
.super Landroid/app/Activity;


# =============================================================================
# SettingsActivity - Main Activity for NVR APK with Steampunk Interface
# =============================================================================


# --- Activity Lifecycle ---
.method public onCreate(Landroid/os/Bundle;)V
    .locals 3
    .param p1, "savedInstanceState"    # Bundle

    # Call super.onCreate
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    # Initialize NexusVkBridge (loads libmobileglues.so)
    invokestatic {}, Lcom/nexus/vulkan/NexusVkBridge;->clinit()V

    # Set Steampunk theme
    const v0, 0x7f0a0000  # R.style.SteampunkTheme_NoActionBar (will be mapped)
    invoke-virtual {p0, v0}, Lcom/nexus/vulkan/SettingsActivity;->setTheme(I)V

    # Set content view to Steampunk layout
    const v0, 0x7f0b0000  # R.layout.activity_settings (will be mapped)
    invoke-virtual {p0, v0}, Lcom/nexus/vulkan/SettingsActivity;->setContentView(I)V

    # Initialize UI components
    invokestatic {p0}, Lcom/nexus/vulkan/SettingsActivity;->initUI(Landroid/app/Activity;)V

    return-void
.end method


# --- Initialize UI Components ---
.method private static initUI(Landroid/app/Activity;)V
    .locals 15
    .param p0, "activity"    # Activity

    # Get references to all UI components
    const v0, 0x7f0c0000  # R.id.sb_cache
    invoke-virtual {p0, v0}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v0
    check-cast v0, Landroid/widget/SeekBar

    const v1, 0x7f0c0001  # R.id.dial_value
    invoke-virtual {p0, v1}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v1
    check-cast v1, Landroid/widget/TextView

    const v2, 0x7f0c0002  # R.id.txt_cache_value
    invoke-virtual {p0, v2}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v2
    check-cast v2, Landroid/widget/TextView

    # Spinners
    const v3, 0x7f0c0010  # R.id.spinner_angle_mode
    invoke-virtual {p0, v3}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v3
    check-cast v3, Landroid/widget/Spinner

    const v4, 0x7f0c0011  # R.id.spinner_gl_version
    invoke-virtual {p0, v4}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v4
    check-cast v4, Landroid/widget/Spinner

    const v5, 0x7f0c0012  # R.id.spinner_hide_mg_env
    invoke-virtual {p0, v5}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v5
    check-cast v5, Landroid/widget/Spinner

    const v6, 0x7f0c0013  # R.id.spinner_multidraw_mode
    invoke-virtual {p0, v6}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v6
    check-cast v6, Landroid/widget/Spinner

    const v7, 0x7f0c0014  # R.id.spinner_angle_depth_clear_fix
    invoke-virtual {p0, v7}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v7
    check-cast v7, Landroid/widget/Spinner

    const v8, 0x7f0c0015  # R.id.spinner_debug_log_level
    invoke-virtual {p0, v8}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v8
    check-cast v8, Landroid/widget/Spinner

    # Switches
    const v9, 0x7f0c0020  # R.id.switch_shader_cache
    invoke-virtual {p0, v9}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v9
    check-cast v9, Landroid/widget/Switch

    const v10, 0x7f0c0021  # R.id.switch_framebuffer_fetch
    invoke-virtual {p0, v10}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v10
    check-cast v10, Landroid/widget/Switch

    const v11, 0x7f0c0022  # R.id.switch_phase2_lighting
    invoke-virtual {p0, v11}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v11
    check-cast v11, Landroid/widget/Switch

    const v12, 0x7f0c0023  # R.id.switch_timer_query
    invoke-virtual {p0, v12}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v12
    check-cast v12, Landroid/widget/Switch

    const v13, 0x7f0c0024  # R.id.switch_compute_shader
    invoke-virtual {p0, v13}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v13
    check-cast v13, Landroid/widget/Switch

    const v14, 0x7f0c0025  # R.id.switch_direct_state_access
    invoke-virtual {p0, v14}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v14
    check-cast v14, Landroid/widget/Switch

    # Setup SeekBar listener for cache size
    new-instance v15, Lcom/nexus/vulkan/SettingsActivity$1;
    invoke-direct {v15, p0, v0, v1, v2}, Lcom/nexus/vulkan/SettingsActivity$1;-><init>(Lcom/nexus/vulkan/SettingsActivity;Landroid/widget/SeekBar;Landroid/widget/TextView;Landroid/widget/TextView;)V
    invoke-virtual {v0, v15}, Landroid/widget/SeekBar;->setOnSeekBarChangeListener(Landroid/widget/SeekBar$OnSeekBarChangeListener;)V

    # Setup Spinner adapters
    invokestatic {p0, v3}, Lcom/nexus/vulkan/SettingsActivity;->setupAngleModeSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    invokestatic {p0, v4}, Lcom/nexus/vulkan/SettingsActivity;->setupGlVersionSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    invokestatic {p0, v5}, Lcom/nexus/vulkan/SettingsActivity;->setupHideMGEnvSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    invokestatic {p0, v6}, Lcom/nexus/vulkan/SettingsActivity;->setupMultidrawModeSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    invokestatic {p0, v7}, Lcom/nexus/vulkan/SettingsActivity;->setupAngleDepthClearFixSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    invokestatic {p0, v8}, Lcom/nexus/vulkan/SettingsActivity;->setupDebugLogLevelSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V

    # Load current settings
    invokestatic {p0, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14}, Lcom/nexus/vulkan/SettingsActivity;->loadCurrentSettings(Landroid/app/Activity;Landroid/widget/SeekBar;Landroid/widget/TextView;Landroid/widget/TextView;Landroid/widget/Spinner;Landroid/widget/Spinner;Landroid/widget/Spinner;Landroid/widget/Spinner;Landroid/widget/Spinner;Landroid/widget/Spinner;Landroid/widget/Switch;Landroid/widget/Switch;Landroid/widget/Switch;Landroid/widget/Switch;Landroid/widget/Switch;Landroid/widget/Switch;)V

    return-void
.end method


# --- SeekBar Change Listener ---
.class Lcom/nexus/vulkan/SettingsActivity$1;
.super Landroid/widget/SeekBar$OnSeekBarChangeListener;

.method public onProgressChanged(Landroid/widget/SeekBar;IZ)V
    .locals 3
    .param p0, "seekBar"    # SeekBar
    .param p1, "progress"   # int
    .param p2, "fromUser"  # boolean

    if-eqz p2, :end

    # Update dial value
    const v0, 0x7f0c0001  # R.id.dial_value
    invoke-virtual {p0, v0}, Landroid/widget/SeekBar;->getRootView()Landroid/view/View;
    move-result-object v0
    invoke-virtual {v0, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v0
    check-cast v0, Landroid/widget/TextView
    invoke-virtual {v0, p1}, Landroid/widget/TextView;->setText(I)V

    # Update cache value text
    const v1, 0x7f0c0002  # R.id.txt_cache_value
    invoke-virtual {p0, v1}, Landroid/widget/SeekBar;->getRootView()Landroid/view/View;
    move-result-object v1
    invoke-virtual {v1, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v1
    check-cast v1, Landroid/widget/TextView
    new-instance v2, Ljava/lang/StringBuilder;
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v3, " MB"
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v2
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v2
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    # Update setting in NexusVkBridge
    invokestatic {p1}, Lcom/nexus/vulkan/NexusVkBridge;->setShaderCacheSize(I)V

    :end
    return-void
.end method


# --- Setup Spinners ---
.method private static setupAngleModeSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    .locals 3
    .param p0, "activity"    # Activity
    .param p1, "spinner"    # Spinner

    new-instance v0, Landroid/widget/ArrayAdapter;
    const/4 v1, 0x1
    new-array v2, [Ljava/lang/String;], 2
    fill-array-data v2, :array_1
    invoke-direct {v0, p0, v1, v2}, Landroid/widget/ArrayAdapter;-><init>(Landroid/content/Context;ILjava/lang/String;)[Ljava/lang/String;]
    invoke-virtual {p1, v0}, Landroid/widget/Spinner;->setAdapter(Landroid/widget/SpinnerAdapter;)V

    return-void

    :array_1
    .array-data 2
        0: "Disabled"
        1: "Enabled"
.end method


.method private static setupGlVersionSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    .locals 3
    .param p0, "activity"    # Activity
    .param p1, "spinner"    # Spinner

    new-instance v0, Landroid/widget/ArrayAdapter;
    const/4 v1, 0x1
    new-array v2, [Ljava/lang/String;], 4
    fill-array-data v2, :array_2
    invoke-direct {v0, p0, v1, v2}, Landroid/widget/ArrayAdapter;-><init>(Landroid/content/Context;ILjava/lang/String;)[Ljava/lang/String;]
    invoke-virtual {p1, v0}, Landroid/widget/Spinner;->setAdapter(Landroid/widget/SpinnerAdapter;)V

    return-void

    :array_2
    .array-data 4
        0: "Default (4.0)"
        1: "OpenGL ES 3.2"
        2: "OpenGL 4.0"
        3: "OpenGL 4.6"
.end method


.method private static setupHideMGEnvSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    .locals 3
    .param p0, "activity"    # Activity
    .param p1, "spinner"    # Spinner

    new-instance v0, Landroid/widget/ArrayAdapter;
    const/4 v1, 0x1
    new-array v2, [Ljava/lang/String;], 2
    fill-array-data v2, :array_3
    invoke-direct {v0, p0, v1, v2}, Landroid/widget/ArrayAdapter;-><init>(Landroid/content/Context;ILjava/lang/String;)[Ljava/lang/String;]
    invoke-virtual {p1, v0}, Landroid/widget/Spinner;->setAdapter(Landroid/widget/SpinnerAdapter;)V

    return-void

    :array_3
    .array-data 2
        0: "Disabled"
        1: "Level 1"
.end method


.method private static setupMultidrawModeSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    .locals 3
    .param p0, "activity"    # Activity
    .param p1, "spinner"    # Spinner

    new-instance v0, Landroid/widget/ArrayAdapter;
    const/4 v1, 0x1
    new-array v2, [Ljava/lang/String;], 6
    fill-array-data v2, :array_4
    invoke-direct {v0, p0, v1, v2}, Landroid/widget/ArrayAdapter;-><init>(Landroid/content/Context;ILjava/lang/String;)[Ljava/lang/String;]
    invoke-virtual {p1, v0}, Landroid/widget/Spinner;->setAdapter(Landroid/widget/SpinnerAdapter;)V

    return-void

    :array_4
    .array-data 6
        0: "Auto"
        1: "Prefer Indirect"
        2: "Prefer Base Vertex"
        3: "Prefer Multidraw Indirect"
        4: "Draw Elements"
        5: "Compute"
.end method


.method private static setupAngleDepthClearFixSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    .locals 3
    .param p0, "activity"    # Activity
    .param p1, "spinner"    # Spinner

    new-instance v0, Landroid/widget/ArrayAdapter;
    const/4 v1, 0x1
    new-array v2, [Ljava/lang/String;], 3
    fill-array-data v2, :array_5
    invoke-direct {v0, p0, v1, v2}, Landroid/widget/ArrayAdapter;-><init>(Landroid/content/Context;ILjava/lang/String;)[Ljava/lang/String;]
    invoke-virtual {p1, v0}, Landroid/widget/Spinner;->setAdapter(Landroid/widget/SpinnerAdapter;)V

    return-void

    :array_5
    .array-data 3
        0: "Disabled"
        1: "Mode 1"
        2: "Mode 2"
.end method


.method private static setupDebugLogLevelSpinner(Landroid/app/Activity;Landroid/widget/Spinner;)V
    .locals 3
    .param p0, "activity"    # Activity
    .param p1, "spinner"    # Spinner

    new-instance v0, Landroid/widget/ArrayAdapter;
    const/4 v1, 0x1
    new-array v2, [Ljava/lang/String;], 5
    fill-array-data v2, :array_6
    invoke-direct {v0, p0, v1, v2}, Landroid/widget/ArrayAdapter;-><init>(Landroid/content/Context;ILjava/lang/String;)[Ljava/lang/String;]
    invoke-virtual {p1, v0}, Landroid/widget/Spinner;->setAdapter(Landroid/widget/SpinnerAdapter;)V

    return-void

    :array_6
    .array-data 5
        0: "Off"
        1: "Error"
        2: "Warn"
        3: "Info"
        4: "Debug"
.end method


# --- Load Current Settings ---
.method private static loadCurrentSettings(Landroid/app/Activity;Landroid/widget/SeekBar;Landroid/widget/TextView;Landroid/widget/TextView;Landroid/widget/Spinner;Landroid/widget/Spinner;Landroid/widget/Spinner;Landroid/widget/Spinner;Landroid/widget/Spinner;Landroid/widget/Spinner;Landroid/widget/Switch;Landroid/widget/Switch;Landroid/widget/Switch;Landroid/widget/Switch;Landroid/widget/Switch;Landroid/widget/Switch;)V
    .locals 17
    .param p0, "activity"    # Activity
    .param p1, "sbCache"     # SeekBar
    .param p2, "dialValue"   # TextView
    .param p3, "txtCacheValue" # TextView
    .param p4, "spAngleMode" # Spinner
    .param p5, "spGlVersion" # Spinner
    .param p6, "spHideEnv"    # Spinner
    .param p7, "spMultidraw" # Spinner
    .param p8, "spDepthClear" # Spinner
    .param p9, "spDebugLog"   # Spinner
    .param p10, "swShaderCache" # Switch
    .param p11, "swFbFetch"    # Switch
    .param p12, "swPhase2"     # Switch
    .param p13, "swTimerQuery" # Switch
    .param p14, "swCompute"    # Switch
    .param p15, "swDSA"       # Switch

    # Get current shader cache size
    invokestatic {}, Lcom/nexus/vulkan/NexusVkBridge;->getShaderCacheSize()I
    move-result v0
    const/32 v1, 32
    if-le v0, v1, :set_min
    goto :check_max
    :set_min
    const/32 v0, 32
    :check_max
    const/32 v1, 128
    if-le v0, v1, :set_seekbar
    const/32 v0, 128
    :set_seekbar
    invoke-virtual {p1, v0}, Landroid/widget/SeekBar;->setProgress(I)V
    invoke-virtual {p2, v0}, Landroid/widget/TextView;->setText(I)V
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v2, " MB"
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v1
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {p3, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    # Set spinners to current values
    const/4 v0, 0x0
    invoke-virtual {p4, v0}, Landroid/widget/Spinner;->setSelection(I)V
    const/4 v0, 0x3
    invoke-virtual {p5, v0}, Landroid/widget/Spinner;->setSelection(I)V
    const/4 v0, 0x0
    invoke-virtual {p6, v0}, Landroid/widget/Spinner;->setSelection(I)V
    const/4 v0, 0x0
    invoke-virtual {p7, v0}, Landroid/widget/Spinner;->setSelection(I)V
    const/4 v0, 0x0
    invoke-virtual {p8, v0}, Landroid/widget/Spinner;->setSelection(I)V
    const/4 v0, 0x0
    invoke-virtual {p9, v0}, Landroid/widget/Spinner;->setSelection(I)V

    # Set switches to current values
    const/4 v0, 0x1
    invoke-virtual {p10, v0}, Landroid/widget/Switch;->setChecked(Z)V
    invoke-virtual {p11, v0}, Landroid/widget/Switch;->setChecked(Z)V
    invoke-virtual {p12, v0}, Landroid/widget/Switch;->setChecked(Z)V
    const/4 v0, 0x0
    invoke-virtual {p13, v0}, Landroid/widget/Switch;->setChecked(Z)V
    invoke-virtual {p14, v0}, Landroid/widget/Switch;->setChecked(Z)V
    invoke-virtual {p15, v0}, Landroid/widget/Switch;->setChecked(Z)V

    return-void
.end method
