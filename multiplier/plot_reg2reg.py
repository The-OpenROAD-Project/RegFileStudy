import sys
import json
import yaml
import matplotlib.pyplot as plt
import numpy as np


def main(argv):
    output = argv[0]
    with open(argv[1], "r") as file:
        study_list = json.load(file)

    study = {study["name"]: study for study in study_list}

    for file in argv[2:]:
        with open(file, "r") as f:
            data = yaml.safe_load(f)

        study[data["name"]] |= data

    # plot screening study from 'study' in matplotlib
    names = list(study.keys())
    inputs = ["width", "latency"]
    exclude = inputs + ["name"]
    variables = set([
        key for name in names for key in study[name].keys()
        if key not in exclude
    ])

    # Plot latency on x axis and reg2reg_min on Y axis
    # {'Multiplier_1': {'name': 'Multiplier_1', 'width': 2, 'latency': 1, 'in2reg_min': 184.33342, 'reg2out_min': 206.094341, 'reg2reg_min': 93.282974, 'instances': 21, 'power': 9.77e-05}, 'Multiplier_2': {'name': 'Multiplier_2', 'width': 2, 'latency': 2, 'in2reg_min': 184.33342, 'reg2out_min': 206.094341, 'reg2reg_min': 93.282974, 'instances': 29, 'power': 0.000125}, 'Multiplier_4': {'name': 'Multiplier_4', 'width': 2, 'latency': 4, 'in2reg_min': 184.33342, 'reg2out_min': 206.094341, 'reg2reg_min': 93.282974, 'instances': 45, 'power': 0.000175}, 'Multiplier_8': {'name': 'Multiplier_8', 'width': 2, 'latency': 8, 'in2reg_min': 184.33342, 'reg2out_min': 206.094341, 'reg2reg_min': 100.725403, 'instances': 78, 'power': 0.000271}, 'Multiplier_16': {'name': 'Multiplier_16', 'width': 2, 'latency': 16, 'in2reg_min': 184.33342, 'reg2out_min': 206.094341, 'reg2reg_min': 97.029221, 'instances': 144, 'power': 0.00046}}

    # Plot latency on x axis and reg2reg_min on Y axis
    plt.figure()
    for name in names:
        plt.scatter(study[name]["latency"], study[name]["reg2reg_min"], label=name)
    plt.xlabel("Latency")
    plt.ylabel("Reg2Reg Min")
    plt.title("Latency vs Reg2Reg Min")
    plt.legend()
    plt.savefig(output)
    plt.close()

if __name__ == "__main__":
    main(sys.argv[1:])
