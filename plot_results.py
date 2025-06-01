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

    # plot instances on y axis and read_ports / write_ports ratio on x axis
    # first gather instances and their read_ports / write_ports ratio
    samples = []
    for s in study.values():
        ratio = s['read_ports'] / s['write_ports']
        samples.append((s['instances'], ratio))
    # now do x,y plot of the instances against their read_ports / write_ports ratio
    instances, ratios = zip(*samples)
    plt.figure(figsize=(10, 6))
    # x,y plot, not bar plots without lines to connect the points
    plt.scatter(ratios, instances, color='blue', marker='o')
    plt.xlabel('Read Ports / Write Ports Ratio')
    plt.title('Instances vs Read Ports / Write Ports Ratio')
    plt.xticks(rotation=45)
    plt.tight_layout()
    
    # plot outputs against eachother in a single .pdf file to look
    # for correlations
    # fig, axs = plt.subplots(len(variables), len(variables), figsize=(15, 15))
    # for i, var1 in enumerate(variables):
    #     for j, var2 in enumerate(variables):
    #         if i == j:
    #             continue
    #         x = [s[var1] for s in study.values()]
    #         y = [s[var2] for s in study.values()]
    #         axs[i, j].scatter(x, y)
    #         axs[i, j].set_xlabel(var1)
    #         axs[i, j].set_ylabel(var2)
    #         axs[i, j].set_xlim(0, averages[var1])
    #         axs[i, j].set_ylim(0, averages[var2])
    #         axs[i, j].set_title(f"{var1} vs {var2}")
    #         axs[i, j].grid(True)
    # plt.suptitle("Study Results Correlation Matrix")
    # plt.subplots_adjust(top=0.9, hspace=0.4, wspace=0.4)
    # plt.xticks(rotation=45)
    # plt.tight_layout(rect=[0, 0, 1, 0.96])  # Adjust layout to fit subtitle
    # plt.tight_layout()
    plt.savefig(output)



if __name__ == "__main__":
    main(sys.argv[1:])
