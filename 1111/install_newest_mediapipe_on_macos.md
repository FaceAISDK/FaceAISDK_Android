## Unable to install mediapipe-model-maker newest version on Macos ?

yes ,when I first time try install mediapipe-model-maker,it can only install 0.1.0.2 ,
but 0.2.1.3 is the newest at 2023,11,14; what the problem ? my macos version is 13.6.1 

So I Post a issues to google/mediaPipe https://github.com/google/mediapipe/issues/4945

Maybe it is a Small probability event，I can not find method to resolve it


## Logs

OS Platform and Distribution
macos Ventura 13.3.1 (a) (22E772610a

Compiler version
pip --version 23.3.1

Programming Language and version
python3 --version 3.9.0


macos Ventura 13.3.1 (a) is not a Stable version？ OK， update from 13.3.1 to 13.6.1

sadly.... 

python -m pip install mediapipe-model-maker==0.2.1.3
Collecting mediapipe-model-maker==0.2.1.3
Using cached mediapipe_model_maker-0.2.1.3-py3-none-any.whl.metadata (1.6 kB)
Requirement already satisfied: absl-py in ./venv/lib/python3.9/site-packages (from mediapipe-model-maker==0.2.1.3) (1.4.0)
INFO: pip is looking at multiple versions of mediapipe-model-maker to determine which version is compatible with other requirements. This could take a while.
ERROR: Could not find a version that satisfies the requirement mediapipe>=0.10.0 (from mediapipe-model-maker) (from versions: 0.8.4.2, 0.8.5, 0.8.6.2, 0.8.7.1, 0.8.7.2, 0.8.7.3, 0.8.8.1, 0.8.9.1, 0.8.10, 0.8.10.1, 0.8.11, 0.9.0, 0.9.0.1, 0.9.1.0)
ERROR: No matching distribution found for mediapipe>=0.10.0

## step 1

  please run below python code 
  ```
   import distutils
   print(distutils.util.get_platform())
   print(platform.mac_ver()[0])
  ```
   the result is :
      macosx-10.9-x86_64
      10.16  
   oh,my God ! why ??

[run below commands on terminal and you will see the difference:](https://eclecticlight.co/2020/08/13/macos-version-numbering-isnt-so-simple/)

SYSTEM_VERSION_COMPAT=1 cat /System/Library/CoreServices/SystemVersion.plist  
SYSTEM_VERSION_COMPAT=0 cat /System/Library/CoreServices/SystemVersion.plist  

   
## Step 2 
run below commands on terminal
export SYSTEM_VERSION_COMPAT=0

You could also add this to your. bash_profile or other shell environment file if you have one, to do this automatically for you.

OK , you can download newest version now 

pip3 install mediapipe-model-maker==newestVersion

Goooooooooooood  Luck!

