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
    inputs = ["rows", "width", "read_ports", "write_ports"]
    exclude = inputs + ["name"]
    variables = set([
        key for name in names for key in study[name].keys()
        if key not in exclude
    ])
    # create dictionary with avarage value for variable in variables
    averages = {
        var: max([s[var] for s in study.values()])
        for var in variables
    }

    # plot instances on y axis and read_ports / write_ports ratio on x axis
    # first gather instances and their read_ports / write_ports ratio
    samples = []
    for s in study.values():
        ratio = s["read_ports"] / s["write_ports"]
        samples.append((s["instances"], ratio))
    # split into two dataseries, one where read_ports > write_ports and one where read_ports < write_ports
    series1 = [(inst, ratio) for inst, ratio in samples if ratio >= 1]
    series2 = [(inst, 1.0 / ratio) for inst, ratio in samples if ratio < 1]
    # now do x,y plot of the instances against their read_ports / write_ports ratio
    # for series1 and series2
    instances1, ratios1 = zip(*series1) if series1 else ([], [])
    instances2, ratios2 = zip(*series2) if series2 else ([], [])

    plt.figure(figsize=(10, 6))
    # x,y plot, not bar plots without lines to connect the points
    plt.scatter(ratios1,
                instances1,
                color="blue",
                label="Read ports & 1 write port)",
                alpha=0.6)
    plt.scatter(ratios2,
                instances2,
                color="red",
                label="Write ports & 1 read port)",
                alpha=0.6)
    plt.xlabel("Port count")
    plt.title("Instances vs register configuration")
    plt.xticks(rotation=45)
    # data series legend lower right
    plt.legend(loc="lower right")
    plt.tight_layout()
    plt.savefig(output)


if __name__ == "__main__":
    main(sys.argv[1:])
