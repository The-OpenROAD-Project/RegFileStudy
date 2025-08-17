load("@bazel-orfs//:openroad.bzl", "orfs_flow", "orfs_run")
load("@bazel-orfs//:write_binary.bzl", "write_binary")
load("@regfilestudy_rules_python//python:pip.bzl", "compile_pip_requirements")
load("chisel.bzl", "chisel_binary")

exports_files(
    [
        "constraints.sdc",
        "results.tcl",
    ],
    visibility = ["//visibility:public"],
)

#load("@rules_cc//cc:defs.bzl", "cc_binary")
#load("@rules_verilator//verilator:defs.bzl", "verilator_cc_library")
#load("@rules_verilator//verilog:defs.bzl", "verilog_library")

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
    $(execpath :regfilestudy) \
    $(location :study) \
    --firtool-binary-path $(execpath @circt//:bin/firtool) -- \
    --default-layer-specialization=disable -o $(location :study.sv)
    """,
    tools = [
        ":regfilestudy",
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
    },
    sources = {
        "SDC_FILE": [":constraints.sdc"],
    },
    verilog_files = [":generate_verilog"],
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
    set -euo pipefail
    $(execpath :plot_results) $(location :plot.pdf) $(location :study) $(locations :results)
    """,
    tools = [
        ":plot_results",
    ],
)
