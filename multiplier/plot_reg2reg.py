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

    # Plot latency on x axis and reg2reg_min on Y axis for retimed and regular
    # Plot latency on x axis and reg2reg_min on Y axis
    plt.figure()

    for name in study:
        if study[name]["retime"] == 1:
            plt.scatter(study[name]["latency"], study[name]["reg2reg_min"], marker='o')
        else:
            plt.scatter(study[name]["latency"], study[name]["reg2reg_min"], marker='x')

    plt.xlabel("Pipeline stages")
    plt.ylabel("Reg2Reg Min")
    plt.title("Pipeline stages vs reg2reg minimum clock period\n'o' retimed, 'x' not retimed")
    plt.legend()
    plt.savefig(output)
    plt.close()

if __name__ == "__main__":
    main(sys.argv[1:])
