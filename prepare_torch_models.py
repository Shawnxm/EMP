import torch
import numpy as np
from math import cos, sin

class TorchMergeModule(torch.nn.Module):
    # def forward(self, points_primary, oxts_primary, points_secondary, oxts_secondary):
        # rotation = self.rotate(oxts_primary, oxts_secondary)
        # translation = self.translate(oxts_primary, oxts_secondary).repeat(points_secondary.shape[0],1)
    def forward(self, points_primary, points_secondary, rotation, translation):
        # translation = translation.repeat(points_secondary.shape[0],1)
        pcl = torch.cat((points_primary, torch.mm(points_secondary, torch.t(rotation)) + translation))
        return pcl

def rotate(oxts1, oxts2):
    ### transformation matrix - rotation (to the perspective of oxts1)
    dYaw = oxts2[5] - oxts1[5]
    dPitch = oxts2[4] - oxts1[4]
    dRoll = oxts2[3] - oxts1[3]
    rotation_Z = torch.tensor([[cos(dYaw), -sin(dYaw), 0, 0], [sin(dYaw), cos(dYaw), 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])
    rotation_Y = torch.tensor([[cos(dPitch), 0, sin(dPitch), 0], [0, 1, 0, 0], [-sin(dPitch), 0, cos(dPitch), 0], [0, 0, 0, 1]])
    rotation_X = torch.tensor([[1, 0, 0, 0], [0, cos(dRoll), -sin(dRoll), 0], [0, sin(dRoll), cos(dRoll), 0], [0, 0, 0, 1]])
    rotation = torch.mm(torch.mm(rotation_Z, rotation_Y), rotation_X)
    return rotation

def translate(oxts1, oxts2):
    ### transformation matrix - translation (to the perspective of oxts1)
    da = oxts2[0] - oxts1[0]  # south --> north
    db = oxts2[1] - oxts1[1]  # east --> west
    dx = da * cos(oxts1[5]) + db * sin(oxts1[5])
    dy = da * -sin(oxts1[5]) + db * cos(oxts1[5])
    dz = oxts2[2] - oxts1[2]
    translation = torch.tensor([dx, dy, dz, 0])
    return translation

if __name__ == '__main__':
    model = TorchMergeModule()
    model.eval()
    
    smod = torch.jit.script(model)
    smod.eval()
    
    smod.save("torch-merge-model.pt1")
    
    loaded = torch.jit.load("torch-merge-model.pt1")
    loaded.eval()

    # test
    pcl1 = np.memmap('src/test/resources/sample_data_for_merging/ego/velodyne/000003.bin', dtype='float32', mode='r').reshape([-1,4])
    pcl2 = np.memmap('src/test/resources/sample_data_for_merging/leftturn/velodyne/000003.bin', dtype='float32', mode='r').reshape([-1,4])
    f = open('src/test/resources/sample_data_for_merging/ego/oxts/000003.txt', 'r')
    oxts1 = [float(x) for x in f.read().split()]
    f.close()
    f = open('src/test/resources/sample_data_for_merging/leftturn/oxts/000003.txt', 'r')
    oxts2 = [float(x) for x in f.read().split()]
    f.close()

    rotation = rotate(oxts1, oxts2)
    print(rotation)
    translation = translate(oxts1, oxts2)
    print(translation)
    pcl = loaded(torch.tensor(pcl1), torch.tensor(pcl2), rotation, translation).numpy()

    # ### save
    save_filename = 'src/test/resources/sample_data_for_merging/mergeResultPy.bin'
    with open(save_filename, 'w') as f:
    	pcl.tofile(f)