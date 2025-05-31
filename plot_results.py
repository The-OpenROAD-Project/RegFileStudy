import sys
import json
import yaml
import matplotlib.pyplot as plt
import numpy as np


def main(argv):
    output = argv[0]
    with open(argv[1], 'r') as file:
        study_list = json.load(file)

    study = {study['name']: study for study in study_list}

    for file in argv[2:]:
        with open(file, 'r') as f:
            data = yaml.safe_load(f)

        study[data['name']] |= data

    # example study data
    # {'RegFile_0_0': {'name': 'RegFile_0_0', 'width': 8, 'rows': 32, 'read_ports': 1, 'write_ports': 1, 'in2reg_arrival': 299.663261, 'reg2out_arrival': 310.048988, 'instances': 1706, 'power': 0.00671}, 'RegFile_0_1': {'name': 'RegFile_0_1', 'width': 8, 'rows': 32, 'read_ports': 1, 'write_ports': 2, 'in2reg_arrival': 326.439857, 'reg2out_arrival': 318.855774, 'instances': 2192, 'power': 0.00792}, 'RegFile_1_0': {'name': 'RegFile_1_0', 'width': 8, 'rows': 32, 'read_ports': 2, 'write_ports': 1, 'in2reg_arrival': 299.663261, 'reg2out_arrival': 310.34207200000003, 'instances': 2108, 'power': 0.00789}, 'RegFile_1_1': {'name': 'RegFile_1_1', 'width': 8, 'rows': 32, 'read_ports': 2, 'write_ports': 2, 'in2reg_arrival': 329.948509, 'reg2out_arrival': 311.553963, 'instances': 2685, 'power': 0.00925}}

    # plot screening study from 'study' in matplotlib
    names = list(study.keys())
    inputs = ["rows", "width", "read_ports", "write_ports"]
    exclude = inputs + ['name']
    variables = set([
        key for name in names for key in study[name].keys()
        if key not in exclude
    ])
    # create dictionary with avarage value for variable in variables
    averages = {
        var: max([s[var] for s in study.values()])
        for var in variables
    }

    # plot outputs against eachother in a single .pdf file to look
    # for correlations
    fig, axs = plt.subplots(len(variables), len(variables), figsize=(15, 15))
    for i, var1 in enumerate(variables):
        for j, var2 in enumerate(variables):
            if i == j:
                continue
            x = [s[var1] for s in study.values()]
            y = [s[var2] for s in study.values()]
            axs[i, j].scatter(x, y)
            axs[i, j].set_xlabel(var1)
            axs[i, j].set_ylabel(var2)
            axs[i, j].set_xlim(0, averages[var1])
            axs[i, j].set_ylim(0, averages[var2])
            axs[i, j].set_title(f"{var1} vs {var2}")
            axs[i, j].grid(True)
    plt.suptitle("Study Results Correlation Matrix")
    plt.subplots_adjust(top=0.9, hspace=0.4, wspace=0.4)
    plt.xticks(rotation=45)
    plt.tight_layout(rect=[0, 0, 1, 0.96])  # Adjust layout to fit suptitle
    plt.tight_layout()
    plt.savefig(output)


if __name__ == "__main__":
    main(sys.argv[1:])
