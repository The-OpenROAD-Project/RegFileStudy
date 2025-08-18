load(":study.bzl", "study")

LATENCIES = [i for i in range(11)]

STUDY = [
    {
        "name": "Multiplier_{i}_{retime}".format(
            i = i,
            retime = retime,
        ),
        "top": "Multiplier_{i}".format(
            i = i,
        ),
        "width": 64,
        "latency": i,
        "retime": retime,
    }
    for i in LATENCIES
    for retime in [
        1,
        0,
    ]
]

INFO = {
    "name": "64x64 multiplier",
    "stage": "synth",
    "study": STUDY,
}

py_binary(
    name = "plot_reg2reg",
    srcs = ["plot_reg2reg.py"],
    deps = [
        "@regfilestudy-pip//matplotlib",
        "@regfilestudy-pip//pyyaml",
        "@regfilestudy-pip//scipy",
    ],
)

study(
    name = "multiplier",
    info = INFO,
    main_class = "GenerateMultiplierStudy",
    scala_files = glob(["src/main/scala/**/*.scala"]),
)
