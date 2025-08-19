load("@bazel-orfs//:openroad.bzl", "orfs_flow", "orfs_run")
load("@bazel-orfs//:write_binary.bzl", "write_binary")
load("@bazel-orfs//toolchains/scala:chisel.bzl", "chisel_binary", "chisel_library")
load("@bazel-orfs//toolchains/scala:scala_bloop.bzl", "scala_bloop")
load("@regfilestudy_rules_python//python:pip.bzl", "compile_pip_requirements")

exports_files(
    [
        "constraints.sdc",
        "results.tcl",
    ],
    visibility = ["//visibility:public"],
)

compile_pip_requirements(
    name = "requirements",
    srcs = ["requirements.in"],
    requirements_txt = "requirements_lock.txt",
)

STUDY = [{
    "name": "RegFile_{i}_{j}".format(
        i = i,
        j = j,
    ),
    "width": 32,
    "rows": 64,
    "read_ports": 1 << i,
    "write_ports": 1 << j,
} for i, j in ([(0, x) for x in range(0, 6)] + [(x, 0) for x in range(1, 6)])]

NAMES = [study["name"] for study in STUDY]

# Write out a .jsonfile with the study parameters.
write_binary(
    name = "study",
    data = str(STUDY),
)

chisel_binary(
    name = "regfilestudy",
    srcs = glob(["src/main/scala/**/*.scala"]),
    main_class = "GenerateRegFileStudy",
    scalacopts = ["-Ytasty-reader"],
    deps = [
        "@maven//:com_github_scopt_scopt_2_13",
        "@maven//:io_circe_circe_yaml_2_13",
        "@maven//:io_circe_circe_yaml_common_2_13",
    ],
)

fir_library(
    name = "generate_verilog_fir",
    data = [
        ":regfilestudy",
        ":study",
    ],
    generator = ":regfilestudy",
    opts = [
        "$(location :study)",
        "--firtool-binary-path $(execpath @circt//:bin/firtool)",
        "--",
        "--default-layer-specialization=disable",
        "-o",
        "$(location :study.sv)",
    ],
    tags = ["manual"],
)

verilog_directory(
    name = "verilog_split",
    srcs = ["generate_verilog_fir"],
    tags = ["manual"],
)

verilog_single_file_library(
    name = "verilog.sv",
    srcs = ["verilog_split"],
    tags = ["manual"],
    visibility = ["//visibility:public"],
)

[orfs_flow(
    name = name,
    # Some simple parameters, we don't care about physical size, we're counting
    # instances.
    arguments = {
        # Speed up the flow by skipping things
        "FILL_CELLS": "",
        "TAPCELL_TCL": "",
        "SKIP_REPORT_METRICS": "1",
        "SKIP_CTS_REPAIR_TIMING": "1",
        "SKIP_INCREMENTAL_REPAIR": "1",
        "GND_NETS_VOLTAGES": "",
        "PWR_NETS_VOLTAGES": "",
        "GPL_ROUTABILITY_DRIVEN": "0",
        "GPL_TIMING_DRIVEN": "0",
        "SETUP_SLACK_MARGIN": "-10000",
        "TNS_END_PERCENT": "0",
        # Normal parameters
        "PLACE_DENSITY": "0.40",
        "CORE_UTILIZATION": "20",
    },
    sources = {
        "SDC_FILE": [":constraints.sdc"],
    },
    verilog_files = [":verilog.sv"],
) for name in NAMES]

[
    orfs_run(
        name = "{base}_results".format(base = name),
        # count instances after floorplan, more than accurate enough and faster
        src = "{name}_floorplan".format(name = name),
        outs = [
            "{name}_stats".format(name = name),
        ],
        arguments = {
            "OUTPUT": "$(location :{name}_stats)".format(name = name),
            "NAME": name,
        },
        script = ":results.tcl",
    )
    for name in NAMES
]

filegroup(
    name = "results",
    srcs = [":{name}_results".format(name = name) for name in NAMES],
    visibility = ["//visibility:public"],
)

py_binary(
    name = "plot_results",
    srcs = ["plot_results.py"],
    deps = [
        "@regfilestudy-pip//matplotlib",
        "@regfilestudy-pip//pyyaml",
    ],
)

genrule(
    name = "plot",
    srcs = [
        "study",
        "results",
    ],
    outs = ["plot.pdf"],
    cmd = """
    set -euox pipefail
    $(execpath :plot_results) $(location :plot.pdf) $(location :study) $(locations :results)
    """,
    tools = [
        ":plot_results",
    ],
)

filegroup(
    name = "chiselfiles",
    srcs = glob(["**/*.scala"]),
    tags = ["manual"],
    visibility = ["//visibility:public"],
)

# This library lists all the scala files directly that
# will be editing
chisel_library(
    name = "blooplib",
    srcs = [
        "//:chiselfiles",
        "//multiplier:chiselfiles",
    ],
    tags = ["manual"],
    deps = [
        "//multiplier:hardfloat",
        "@maven//:org_scalatest_scalatest_2_13",
    ],
)

# One bloop to rule them all
scala_bloop(
    name = "bloop",
    src = "blooplib",
)
