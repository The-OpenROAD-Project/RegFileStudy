load("@bazel-orfs//:openroad.bzl", "orfs_flow", "orfs_run")
load("@bazel-orfs//:write_binary.bzl", "write_binary")
load("@openroad_rules_python//python:pip.bzl", "compile_pip_requirements")
load("chisel.bzl", "chisel_binary")

#load("@rules_cc//cc:defs.bzl", "cc_binary")
#load("@rules_verilator//verilator:defs.bzl", "verilator_cc_library")
#load("@rules_verilator//verilog:defs.bzl", "verilog_library")

compile_pip_requirements(
    name = "requirements",
    srcs = ["requirements.in"],
    requirements_txt = "requirements_lock.txt",
)

STUDY = [{
    "name": "RegFile_{i}_{j}_{k}".format(
        i = i,
        j = j,
        k = k,
    ),
    "width": 16,
    "rows": 1 << k,
    "read_ports": 1 << i,
    "write_ports": 1 << j,
} for i in range(0, 5) for j in range(0, 5) for k in range(0, 5)]

NAMES = [study["name"] for study in STUDY]

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
        "@openroad_maven//:com_chuusai_shapeless_2_13",
        "@openroad_maven//:com_github_scopt_scopt_2_13",
        "@openroad_maven//:io_circe_circe_core_2_13",
        "@openroad_maven//:io_circe_circe_generic_2_13",
        "@openroad_maven//:io_circe_circe_numbers_2_13",
        "@openroad_maven//:io_circe_circe_yaml_2_13",
        "@openroad_maven//:io_circe_circe_yaml_common_2_13",
        "@openroad_maven//:org_typelevel_cats_core_2_13",
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
    abstract_stage = "cts",
    # Some simple parameters, we don't care about physical size, we're counting
    # instances.
    arguments = {
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
        src = "{name}_cts".format(name = name),
        outs = [
            "{name}_stats".format(name = name),
        ],
        arguments = {
            "OUTPUT": "$(location :{name}_stats)".format(name = name),
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
        "@openroad-pip//matplotlib",
        "@openroad-pip//pyyaml",
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
