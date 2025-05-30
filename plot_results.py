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
    print(averages)
    print(study)
    # Now create a plot with one plot per variable in variables
    n_vars = len(variables)
    n_cols = 2
    n_rows = (n_vars + n_cols - 1) // n_cols
    fig, axs = plt.subplots(n_rows, n_cols, figsize=(12, 6 * n_rows))
    axs = axs.flatten() if n_rows > 1 else [axs]
    for i, var in enumerate(variables):
        values = [s[var] for s in study.values()]
        # Not a bar chart, but show individual values axs[i].bar(names, values)
        axs[i].scatter(names, values, label=var)
        axs[i].set_title(var)
        axs[i].set_xticklabels(names, rotation=45, ha='right')
        axs[i].set_ylabel(var)
        axs[i].set_xlabel('Study Name')
        axs[i].grid(True)
    plt.tight_layout()
    plt.savefig(output)


if __name__ == "__main__":
    main(sys.argv[1:])
