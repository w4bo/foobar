# -*- coding: utf-8 -*-
import os
import matplotlib.pyplot as plt
import matplotlib.font_manager as font_manager
import pandas as pd
import numpy as np
import codecs

#==============================================================================
# Chart variables
#==============================================================================
titlesize = 16
subtitlesize = 14
labelsize = 14
axessize = 12
legendsize = 11
markersize = 5

# http://scipy-cookbook.readthedocs.io/items/Matplotlib_LaTeX_Examples.html
plt.rcParams.update(plt.rcParamsDefault)
# plt.style.use('grayscale')
# plt.rc('text', usetex=True)
plt.rc('font', family='serif')
plt.rcParams['mathtext.fontset'] = 'dejavuserif'
font = font_manager.FontProperties(family='serif', size=legendsize)

# You typically want your plot to be ~1.33x wider than tall. This plot is a rare
# exception because of the number of lines being plotted on it.
# Common sizes: (10, 7.5) and (12, 9)
# Make room for the ridiculously large title.
# plt.subplots_adjust(top=0.8)
figsize = (12,9)
# figsize = (12,3)

# Markers
# https://matplotlib.org/api/markers_api.html
# Lines
# https://matplotlib.org/gallery/lines_bars_and_markers/line_styles_reference.html

# =============================================================================
# These are the "Tableau 20" colors as RGB.
# http://www.randalolson.com/2014/06/28/how-to-make-beautiful-data-visualizations-in-python-with-matplotlib/
# =============================================================================

# =============================================================================
# Location String	Location Code
# 'best'	0
# 'upper right'	1
# 'upper left'	2
# 'lower left'	3
# 'lower right'	4
# 'right'	5
# 'center left'	6
# 'center right'	7
# 'lower center'	8
# 'upper center'	9
# 'center'	10
# =============================================================================
markers = ["v", "^", "<", ">", "8", "s", "p", "P", "*", "+", "X", "D", "o", "s"]
tableau20 = [(31, 119, 180), (174, 199, 232), (255, 127, 14), (255, 187, 120),
             (44, 160, 44), (152, 223, 138), (214, 39, 40), (255, 152, 150),
             (148, 103, 189), (197, 176, 213), (140, 86, 75), (196, 156, 148),
             (227, 119, 194), (247, 182, 210), (127, 127, 127), (199, 199, 199),
             (188, 189, 34), (219, 219, 141), (23, 190, 207), (158, 218, 229)]

# Scale the RGB values to the [0, 1] range, which is the format matplotlib accepts.
for i in range(len(tableau20)):
    r, g, b = tableau20[i]
    tableau20[i] = (r / 255., g / 255., b / 255.)

#==============================================================================
path =    '../../../resources/test/'
outpath = '../../../resources/test/charts/'

filename = "test.csv"
simMember, simMeta, synMember, synMeta, nsize, covThr, distThr = 0.9, 0.4, 1, 5, 3, 0.6, 3

def marker(filename):
    # if 'EXT' in filename:
    return 6, 'o', '-', 'C-BI', 'black'
    # else:
    #     c = 'gray'
    #     m = "^"
    #     if th == 0.05:
    #         c = 'silver'
    #         m = "v"
    #     elif th == 0.25:
    #         c = 'dimgray'
    #         m = "<"
    #     return 6, m, '--', 'A-BI$_{' + str(theta) + '}$', c

###########################################################################
# Effectiveness - similarity
###########################################################################
fig, ax = plt.subplots(1, 1, figsize=(4, 3))
with codecs.open(path + filename, 'r', encoding='utf-8') as f:
    msize, m, ls, l, c = marker(filename)    
    data = pd.read_csv(f, sep=';')
    ddata = data
    ddata = ddata[ddata["simMember"] == simMember]
    ddata = ddata[ddata["simMeta"] == simMeta]
    ddata = ddata[ddata["synMember"] == synMember]
    ddata = ddata[ddata["synMeta"] == synMeta]
    ddata = ddata[ddata["%missing"] == covThr]
    ddata = ddata[ddata["maxDistance"] == distThr]
    ddata = ddata[ddata["ngramSize"] == nsize]
    d = ddata.groupby(["k"])
    k = list(d.groups.keys())
    ax.set_ylabel("$Sim$", fontsize=axessize)
    ax.plot(k, d['similarity'].mean(), label = l, marker = m, markersize = msize, linestyle = ls, fillstyle='none', linewidth=0.8, color=c) # label="$q_{div}$",
    ax.set_xticks(k)
ax.grid(color="lightgray", linestyle='-', linewidth=0.2)
ax.set_axisbelow(True)
ax.set_xlabel("$k$", fontsize=axessize)
ax.set_ylim([0, 1])
ax.legend(handletextpad=0, columnspacing = 0.2, labelspacing=0.2, frameon=False, fontsize=legendsize, ncol=2)
fig.tight_layout()
# fig.savefig(outpath + 'similarity_avg.pdf')
# fig.show()
        
###########################################################################
# Effectiveness - similarity top k
###########################################################################
fig, ax = plt.subplots(1, 1, figsize=(4, 3))
with codecs.open(path + filename, 'r', encoding='utf-8') as f:
    msize, m, ls, l, c = marker(filename)    
    data = pd.read_csv(f, sep=';')
    ddata = data
    ddata = ddata[ddata["simMember"] == simMember]
    ddata = ddata[ddata["simMeta"] == simMeta]
    ddata = ddata[ddata["synMember"] == synMember]
    ddata = ddata[ddata["%missing"] == covThr]
    ddata = ddata[ddata["maxDistance"] == distThr]
    ddata = ddata[ddata["ngramSize"] == nsize]
    ks = [1, 2, 3, 4, 5]
    res = []
    width = 0         # the width of the bars
    incwidth = 0.2
    ind = 0
    for s in [1, 3, 5]:
        res = []
        for k in ks:
            dddata = ddata
            dddata = dddata[dddata["synMeta"] == s]
            dddata = dddata[dddata["k"] <= k]
            d = dddata.groupby(["id"])
            res.append([k, d['similarity'].max().mean()])
        if s == 3:
            m = "s"
            ls = "--"
            c = 'grey'
        elif s == 5:
            m = "x"
            ls = "-."
            c = 'darkgrey'
        else: 
            m = "o"
            ls = "-"
            c = 'black'
        # ax.plot([x[0] for x in res], [x[1] for x in res], label = l + "$_ " + str((s + 1)) + "$", marker = m, markersize = msize, linestyle = ls, fillstyle='none', linewidth=0.8, color=c) # label="$q_{div}$",
        ind = np.arange(len(res))   # the x locations for the groups
        ax.bar(ind + width, [x[1] for x in res], width=incwidth, color=c, bottom=0, label="SynThr = $" + str((s + 1)) + "$")
        width = width + incwidth
    ax.set_xticks(ind + incwidth)
    ax.set_xticklabels([x[0] for x in res])
    ax.set_ylabel("$Sim$", fontsize=axessize)
ax.grid(color="lightgray", linestyle='-', linewidth=0.2)
ax.set_axisbelow(True)
ax.set_xlabel("$k$", fontsize=axessize)
ax.set_ylim([0, 1])
ax.legend(handletextpad=0, columnspacing = 0.2, labelspacing=0.2, frameon=False, fontsize=legendsize, ncol=2)
fig.tight_layout()
fig.savefig(outpath + 'similarity.pdf')
fig.show()

###########################################################################
# Efficiency - pruning
###########################################################################
fig, ax = plt.subplots(1, 1, figsize=(4, 3))
with codecs.open(path + filename, 'r', encoding='utf-8') as f:
    msize, m, ls, l, c = marker(filename)    
    data = pd.read_csv(f, sep=';')
    ddata = data
    ddata = ddata[ddata["simMember"] == simMember]
    ddata = ddata[ddata["simMeta"] == simMeta]
    ddata = ddata[ddata["synMember"] == synMember]
    ddata = ddata[ddata["synMeta"] == synMeta]
    ddata = ddata[ddata["%missing"] == covThr]
    ddata = ddata[ddata["maxDistance"] == distThr]
    ddata = ddata[ddata["ngramSize"] == nsize]
    ddata = ddata[ddata["ngrams_count"] % 2 == 0]
    ddata = ddata[ddata["ngrams_count"] > 0]
    d = ddata.groupby(["ngrams_count"])
    k = list(d.groups.keys())
    ax.set_ylabel("Explored mappings", fontsize=axessize)
    # ax.plot(k, d['sentence_count'].mean() / 1000, marker = "x", markersize = msize, linestyle = "--", fillstyle='none', linewidth=0.8, color=c, label="No pruning") # ",
    # ax.plot(k, d['sentence_count_pruned'].mean() / 1000, marker = "s", markersize = msize, linestyle = "-.", fillstyle='none', linewidth=0.8, color=c, label="Distinct mappings") # label="$q_{div}$",
    # ax.plot(k, d['sentence_pruned'].mean() / 1000, marker = m, markersize = msize, linestyle = ls, fillstyle='none', linewidth=0.8, color=c, label="Computed mappings") # label="$q_{div}$",
    # ax.set_xticks(k)
    ind = np.arange(len(k))    # the x locations for the groups
    width = 0.2         # the width of the bars
    ax.bar(ind, d['sentence_count'].mean(), width=width, color='darkgrey', bottom=0, label="All") # ",
    ax.bar(ind + width, d['sentence_count_pruned'].mean(), width=width, color='grey', bottom=0, label="Distinct") # label="$q_{div}$",
    ax.bar(ind + width * 2, d['sentence_pruned'].mean(), width=width, color='black', bottom=0, label="Computed") # label="$q_{div}$",
    ax.set_xticks(ind + width)
    ax.set_xticklabels(k)
ax.grid(color="lightgray", linestyle='-', linewidth=0.2)
ax.set_axisbelow(True)
ax.set_xlabel("$|M|$", fontsize=axessize)
ax.set_yscale('log')
ax.legend(handletextpad=0, columnspacing = 0.2,  labelspacing=0.2, frameon=False, fontsize=legendsize, ncol=1)
fig.tight_layout()
fig.savefig(outpath + 'pruning.pdf')
fig.show()

###########################################################################
# Efficiency - Time (s)
###########################################################################
fig, ax = plt.subplots(1, 1, figsize=(4, 3))
with codecs.open(path + filename, 'r', encoding='utf-8') as f:
    msize, m, ls, l, c = marker(filename)    
    data = pd.read_csv(f, sep=';')
    ddata = data
    ddata = ddata[ddata["simMember"] == simMember]
    ddata = ddata[ddata["simMeta"] == simMeta]
    ddata = ddata[ddata["synMember"] == synMember]
    ddata = ddata[ddata["%missing"] == covThr]
    ddata = ddata[ddata["maxDistance"] == distThr]
    ddata = ddata[ddata["ngramSize"] == nsize]
    ddata = ddata[ddata["ngrams_count"] % 2 == 0]
    ddata = ddata[ddata["ngrams_count"] > 0]
    ax.set_ylabel("Time (s)", fontsize=axessize)
    # ax.plot(k, (d['lemmatization_time'].mean() + d['match_time'].mean() + d['sentence_time'].mean()) / 1000, marker = m, markersize = msize, linestyle = ls, fillstyle='none', linewidth=0.8, color=c, label=l) # ",
    # ax.set_xticks(k)
    res = []
    width = 0         # the width of the bars
    incwidth = 0.2
    for s in [1, 3, 5]:
        dddata = ddata
        dddata = dddata[dddata["synMeta"] == s]
        d = dddata.groupby(["ngrams_count"])
        k = list(d.groups.keys())
        if s == 3:
            m = "s"
            ls = "--"
            c = 'grey'
        elif s == 5:
            m = "x"
            ls = "-."
            c = 'darkgrey'
        else: 
            m = "o"
            ls = "-"
            c = 'black'
        # ax.plot([x[0] for x in res], [x[1] for x in res], label = l + "$_ " + str((s + 1)) + "$", marker = m, markersize = msize, linestyle = ls, fillstyle='none', linewidth=0.8, color=c) # label="$q_{div}$",
        ind = np.arange(len(k))   # the x locations for the groups
        ax.bar(ind + width, (d['lemmatization_time'].mean() + d['match_time'].mean() + d['sentence_time'].mean()) / 1000, width=incwidth, bottom=0, color=c, label="SynThr = $" + str((s + 1)) + "$") # ",
        width = width + incwidth
    ax.set_xticks(ind + incwidth)
    ax.set_xticklabels(k)
ax.grid(color="lightgray", linestyle='-', linewidth=0.2)
ax.set_axisbelow(True)
ax.set_xlabel("$|M|$", fontsize=axessize)
ax.legend(handletextpad=0, columnspacing = 0.2, labelspacing=0.2, frameon=False, fontsize=legendsize)
fig.tight_layout()
fig.savefig(outpath + 'time.pdf')
fig.show()