load("@bazel-orfs//toolchains/scala:chisel.bzl", "chisel_library")
load(":study.bzl", "study")

LATENCIES = [i for i in range(11)]

INFO = {
    "name": "64x64 multiplier",
    "stage": "synth",
    "study": [
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
    ],
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

chisel_library(
    name = "hardfloat",
    srcs = [
        "@berkeley_hardfloat//:hardfloat/src/main/scala/AddRecFN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/CompareRecFN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/INToRecFN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/MulRecFN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/RecFNToIN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/RecFNToRecFN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/RoundAnyRawFNToRecFN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/common.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/fNFromRecFN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/primitives.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/rawFloatFromFN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/rawFloatFromIN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/rawFloatFromRecFN.scala",
        "@berkeley_hardfloat//:hardfloat/src/main/scala/recFNFromFN.scala",
    ],
    visibility = ["//visibility:public"],
)

MULTIPLIER_INFO = {
    "name": "FPU",
    "stage": "synth",
    "study": [
        {
            "name": "FloatingPointUnit_{i}_{retime}".format(
                i = i,
                retime = retime,
            ),
            "top": "FloatingPointUnit_{i}".format(
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
    ],
}

study(
    name = "fpu",
    info = MULTIPLIER_INFO,
    main_class = "GenerateFloatingPointUnitStudy",
    scala_files = glob(["src/main/scala/**/*.scala"]),
)

filegroup(
    name = "chiselfiles",
    srcs = glob(["**/*.scala"]),
    tags = ["manual"],
    visibility = ["//visibility:public"],
)
