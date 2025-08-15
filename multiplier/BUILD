load("@bazel-orfs//:openroad.bzl", "orfs_flow", "orfs_run")
load("@bazel-orfs//:write_binary.bzl", "write_binary")
load("@regfilestudy_rules_python//python:pip.bzl", "compile_pip_requirements")
load("//:chisel.bzl", "chisel_binary")

#load("@rules_cc//cc:defs.bzl", "cc_binary")
#load("@rules_verilator//verilator:defs.bzl", "verilator_cc_library")
#load("@rules_verilator//verilog:defs.bzl", "verilog_library")

LATENCIES = [i for i in range(15)]

STUDY = [{
    "name": "Multiplier_{i}".format(
        i = i,
    ),
    "width": 64,
    "latency": i,
} for i in LATENCIES]

NAMES = [study["name"] for study in STUDY]

# Write out a .jsonfile with the study parameters.
write_binary(
    name = "study",
    data = str(STUDY),
)

chisel_binary(
    name = "multiplierstudy",
    srcs = glob(["src/main/scala/**/*.scala"]),
    main_class = "GenerateMultiplierStudy",
    scalacopts = ["-Ytasty-reader"],
    deps = [
        "@regfilestudy_maven//:com_chuusai_shapeless_2_13",
        "@regfilestudy_maven//:com_github_scopt_scopt_2_13",
        "@regfilestudy_maven//:io_circe_circe_core_2_13",
        "@regfilestudy_maven//:io_circe_circe_generic_2_13",
        "@regfilestudy_maven//:io_circe_circe_numbers_2_13",
        "@regfilestudy_maven//:io_circe_circe_yaml_2_13",
        "@regfilestudy_maven//:io_circe_circe_yaml_common_2_13",
        "@regfilestudy_maven//:org_typelevel_cats_core_2_13",
    ],
)

genrule(
    name = "generate_verilog",
    srcs = [],
    outs = [
        "study.sv",
    ],
    cmd = """
    $(execpath :multiplierstudy) \
    $(location :study) \
    --firtool-binary-path $(execpath @circt//:bin/firtool) -- \
    --default-layer-specialization=disable -o $(location :study.sv)
    """,
    tools = [
        ":multiplierstudy",
        ":study",
        "@circt//:bin/firtool",
    ],
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
        "SYNTH_RETIME_MODULES": name,
    },
    sources = {
        "SDC_FILE": ["//:constraints.sdc"],
    },
    verilog_files = [":generate_verilog"],
) for name in NAMES]

[
    orfs_run(
        name = "{base}_results".format(base = name),
        # count instances after cts, more than accurate enough and faster
        src = "{name}_cts".format(name = name),
        outs = [
            "{name}_stats".format(name = name),
        ],
        arguments = {
            "OUTPUT": "$(location :{name}_stats)".format(name = name),
        },
        script = "//:results.tcl",
    )
    for name in NAMES
]

filegroup(
    name = "results",
    srcs = [":{name}_results".format(name = name) for name in NAMES],
    visibility = ["//visibility:public"],
)

py_binary(
    name = "plot_reg2reg",
    srcs = ["plot_reg2reg.py"],
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
    set -euo pipefail
    $(execpath :plot_reg2reg) $(location :plot.pdf) $(location :study) $(locations :results)
    """,
    tools = [
        ":plot_reg2reg",
    ],
)
