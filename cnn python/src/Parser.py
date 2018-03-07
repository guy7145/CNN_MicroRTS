import json
from multiprocessing.pool import Pool
import numpy as np
import os
import math

WIDTH, HEIGHT, TERRAIN, PLAYERS, UNITS = 'width', 'height', 'terrain', 'players', 'units'
PLAYER_ID, PLAYER_RESOURCES = 'ID', 'resources'
UNIT_TYPE, UNIT_ID, UNIT_PLAYER, UNIT_X, UNIT_Y, UNIT_HP = 'type', 'ID', 'player', 'x', 'y', 'hitpoints'

ALL_UNIT_TYPES = ['BASE', 'BARRACKS', 'WORKER', 'LIGHT', 'HEAVY', 'RANGED', 'RESOURCE']
ONEHOT_SIZE = None

min_units_for_player = math.inf

labeled_player = 0


def ignore_enemy_setter(unit, map):
    if unit[UNIT_PLAYER] != labeled_player:
        return
    i = ALL_UNIT_TYPES.index(unit[UNIT_TYPE].upper())
    map[unit[UNIT_Y], unit[UNIT_X], i] = 1
    return


def indicate_player_setter(unit, map):
    i = ALL_UNIT_TYPES.index(unit[UNIT_TYPE].upper())
    map[unit[UNIT_Y], unit[UNIT_X], i] = 1
    map[unit[UNIT_Y], unit[UNIT_X], ONEHOT_SIZE - 1] = {0: 1, -1: 0}.get(unit[UNIT_PLAYER], -1)
    return


def set_unit_onehot_in_map(*args, **kwargs):
    raise NotImplementedError()


def parse(json_dict):
    h, w = json_dict[WIDTH], json_dict[HEIGHT]
    terrain = [str(c) for c in json_dict[TERRAIN]]
    # map = np.array(terrain).reshape((h, w))
    map = np.ndarray((h, w, ONEHOT_SIZE))
    map.fill(0)
    units = json_dict[UNITS]
    c = 0
    for unit in units:
        if unit[UNIT_PLAYER] == labeled_player:
            c += 1
        set_unit_onehot_in_map(unit, map)
    return map, c


def load_and_parse(path):
    with open(path, 'r', encoding='utf-8') as f:
        label = path[:path.rfind('\\')]
        label = label[label.rfind('\\') + 1:]
        return parse(json.load(f)), label


def iter_samples(path):
    def iter_files_in_path():
        for root, _, files in os.walk(path):
            for f in files[500:2500]:
                yield os.path.join(root, f)

    global ONEHOT_SIZE
    if set_unit_onehot_in_map == indicate_player_setter:
        ONEHOT_SIZE = len(ALL_UNIT_TYPES) + 1
    elif set_unit_onehot_in_map == ignore_enemy_setter:
        ONEHOT_SIZE = len(ALL_UNIT_TYPES) - 1
    else:
        raise Exception('no onehot parsing function set')

    global min_units_for_player
    for p in iter_files_in_path():
        (x, c), y = load_and_parse(p)
        if c < min_units_for_player:
            continue
        yield x, y
    return
