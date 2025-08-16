import sys
import json
import yaml
import matplotlib.pyplot as plt
import numpy as np

from scipy.optimize import curve_fit  # Add this import at the top

def inv_func_thru_zero(x, a, y0):
    return a / (x + 1) + y0 - a

def main(argv):
    output = argv[0]
    with open(argv[1], "r") as file:
        info = json.load(file)

    study = {study["name"]: study for study in info["study"]}

    for file in argv[2:]:
        with open(file, "r") as f:
            data = yaml.safe_load(f)
        study[data["name"]] |= data

    # Separate data for retimed and not retimed
    latency_retimed = []
    reg2reg_min_retimed = []
    latency_regular = []
    reg2reg_min_regular = []

    for name in study:
        latency = study[name]["latency"]
        reg2reg_min = study[name]["reg2reg_min"]
        if study[name]["retime"] == 1:
            latency_retimed.append(latency)
            reg2reg_min_retimed.append(reg2reg_min)
        else:
            latency_regular.append(latency)
            reg2reg_min_regular.append(reg2reg_min)

    plt.figure()

    # Plot points
    plt.scatter(latency_retimed, reg2reg_min_retimed, marker='o', label='Retimed')
    plt.scatter(latency_regular, reg2reg_min_regular, marker='x', label='Not Retimed')

    # Add trendlines
    if latency_retimed:
        x = np.array(latency_retimed)
        y = np.array(reg2reg_min_retimed)
        if 0 in x:
            y0 = y[x == 0][0]
        else:
            y0 = min(y)
        mask = x != 0
        x_fit = x[mask]
        y_fit = y[mask]
        def fit_func(x, a):
            return inv_func_thru_zero(x, a, y0)
        popt, _ = curve_fit(
            fit_func,
            x_fit,
            y_fit,
            p0=[1]
        )
        x_plot = np.linspace(0, max(x_fit), 100)
        plt.plot(x_plot, fit_func(x_plot, *popt), 'b--', label='Retimed 1/(x+1) Fit (thru 0)')
        plt.scatter([0], [y0], color='b', marker='o', label='Retimed x=0')

    if latency_regular:
        z_regular = np.polyfit(latency_regular, reg2reg_min_regular, 1)
        p_regular = np.poly1d(z_regular)
        plt.plot(sorted(latency_regular), p_regular(sorted(latency_regular)), 'r--', label='Not Retimed Trend')

    plt.xlabel("Pipeline stages")
    plt.ylabel("Reg2Reg Min")
    plt.title(f"{info['stage']} minimum clock period")
    plt.legend()
    plt.savefig(output)
    plt.close()

if __name__ == "__main__":
    main(sys.argv[1:])
