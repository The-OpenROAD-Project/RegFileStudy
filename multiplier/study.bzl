"""
Study retiming utility function
"""

load("@bazel-orfs//:generate.bzl", "fir_library", "verilog_directory", "verilog_single_file_library")
load("@bazel-orfs//:openroad.bzl", "orfs_flow", "orfs_run")
load("@bazel-orfs//:write_binary.bzl", "write_binary")
load("@bazel-orfs//toolchains/scala:chisel.bzl", "chisel_binary")

def study(name, info, scala_files, module):
    """
    Create a study for the given parameters.

    Args:
        name: The name of the study.
        info: The information about the study.
        scala_files: The Scala files to include in the study.
        module: The module to use for the study.
    """

    # Write out a .jsonfile with the study parameters.
    write_binary(
        name = "{name}_study".format(name = name),
        data = str(info),
    )

    chisel_binary(
        name = "{name}_generate_study".format(name = name),
        srcs = scala_files,
        main_class = "GenerateStudy",
        scalacopts = ["-Ytasty-reader"],
        deps = [
            "@maven//:com_github_scopt_scopt_2_13",
            "@maven//:io_circe_circe_yaml_2_13",
            "@maven//:io_circe_circe_yaml_common_2_13",
            "//multiplier:hardfloat",
        ],
    )

    fir_library(
        name = "{name}_fir".format(name = name),
        data = [
            ":{name}_generate_study".format(name = name),
            ":{name}_study".format(name = name),
        ],
        generator = "{name}_generate_study".format(name = name),
        opts = [
            "{module}".format(module = module),
            "$(location :{name}_study)".format(name = name),
            "--",
            "--",
            "--default-layer-specialization=disable",
        ],
        tags = ["manual"],
    )

    verilog_directory(
        name = "{name}_verilog_split".format(name = name),
        srcs = ["{name}_fir".format(name = name)],
        tags = ["manual"],
    )

    verilog_single_file_library(
        name = "{name}.sv".format(name = name),
        srcs = ["{name}_verilog_split".format(name = name)],
        tags = ["manual"],
        visibility = ["//visibility:public"],
    )

    for study in info["study"]:
        orfs_flow(
            name = study["name"],
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
                "SYNTH_KEEP_MODULES": (study["top"] + "_core") if study["retime"] == 1 else "",
                "SYNTH_RETIME_MODULES": (study["top"] + "_core") if study["retime"] == 1 else "",
            },
            sources = {
                "SDC_FILE": ["//:constraints.sdc"],
            },
            top = study["top"],
            variant = "retimed" if study["retime"] == 1 else "base",
            verilog_files = [":{name}.sv".format(name = name)],
        )

        orfs_run(
            name = "{name}_results".format(
                name = study["name"],
            ),
            # count instances after cts, more than accurate enough and faster
            src = "{name}_{variant}{stage}".format(
                name = study["name"],
                stage = info["stage"],
                variant = "retimed_" if study["retime"] == 1 else "",
            ),
            outs = [
                "{name}_stats".format(
                    name = study["name"],
                ),
            ],
            arguments = {
                "NAME": study["name"],
                "OUTPUT": "$(location :{name}_stats)".format(
                    name = study["name"],
                ),
            },
            script = "//:results.tcl",
        )

    native.filegroup(
        name = "{name}_results".format(name = name),
        srcs = [":{study_name}_results".format(
            study_name = study["name"],
        ) for study in info["study"]],
        visibility = ["//visibility:public"],
    )

    native.genrule(
        name = "{}_plot".format(name),
        srcs = [
            "{name}_study".format(name = name),
            "{name}_results".format(name = name),
        ],
        outs = ["{name}_plot.pdf".format(name = name)],
        cmd = """
        set -euo pipefail
        $(execpath :plot_reg2reg) $(location :{name}_plot.pdf) $(location :{name}_study) $(locations :{name}_results)
        """.format(name = name),
        tools = [
            "//multiplier:plot_reg2reg",
        ],
    )
