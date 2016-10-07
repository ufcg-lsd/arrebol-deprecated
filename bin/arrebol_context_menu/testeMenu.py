import os, sys, glob, subprocess, urllib
from gi.repository import Nautilus, GObject, Gio
from Tkinter import *

class ColumnExtension(GObject.GObject, Nautilus.MenuProvider):

    userName = ''
    userPassword = ''
    root = Tk()
    user = StringVar()
    password = StringVar()

    def __init__(self):
        pass

    def authenticateUser(self, *args):
    	os.system("echo 'Authenticating' >> /tmp/log_menu.log")
    	os.system("echo 'User:"+str(self.user.get())+" -- Pass:"+str(self.password.get())+"' >> /tmp/log_menu.log")
    	self.userName = str(self.user.get())
    	self.userPassword = str(self.password.get())
    	self.root.destroy()
    	self.root = None

    def getUserCredentials(self):

		#def authenticateUser():
		os.system("echo 'Initiating Authentication' >> /tmp/log_menu.log")
		if self.root is None:
			self.root = Tk()
		self.root.title("Your Credentials")

		w = 300 # width for the Tk root
		h = 150 # height for the Tk root

		# get screen width and height
		ws = self.root.winfo_screenwidth() # width of the screen
		hs = self.root.winfo_screenheight() # height of the screen

		# calculate x and y coordinates for the Tk root window
		x = (ws/2) - (w/2)
		y = (hs/2) - (h/2)

		mainframe = Frame(self.root)
		mainframe.grid(column=0, row=0, sticky=(N, W, E, S))
		mainframe.columnconfigure(0, weight=1)
		mainframe.rowconfigure(0, weight=1)

		user_entry = Entry(mainframe, width=20, textvariable=self.user)
		user_entry.grid(column=2, row=1, sticky=(W, E))
		pass_entry = Entry(mainframe, width=20, textvariable=self.password, show="*")
		pass_entry.grid(column=2, row=2, sticky=(W, E))

		#Label(mainframe, textvariable=password).grid(column=2, row=2, sticky=(W, E))
		Button(mainframe, text="Authenticate", command=self.authenticateUser).grid(column=2, row=3, sticky=W)

		Label(mainframe, text="User").grid(column=1, row=1, sticky=W)
		Label(mainframe, text="Password").grid(column=1, row=2, sticky=W)

		for child in mainframe.winfo_children(): child.grid_configure(padx=5, pady=5)

		user_entry.focus()
		#root.bind('<Return>', calculate)

		# set the dimensions of the screen 
		# and where it is placed
		self.root.geometry('%dx%d+%d+%d' % (w, h, x, y))
		self.root.mainloop()

    def menu_activate_execute(self, menu, selectedFile):
		os.system("echo 'Execution called' >> /tmp/log_menu.log")
		os.system("echo 'Self: "+str(self)+"' >> /tmp/log_menu.log")
		#os.system("arrebol POST "+fileCompleteName+" --username "+userName)
		try:
			os.system("echo 'Call authenticator' >> /tmp/log_menu.log")
			self.getUserCredentials()
		except TypeError, e:
			os.system("echo '"+str(e)+"' >> /tmp/log_menu.log")

		os.system("echo 'Executing action for User:'"+self.userName+" -- Pass: "+self.userPassword+"' >> /tmp/log_menu.log")

		fileCompleteName = str(selectedFile.get_uri())[7:]
		os.system("gvfs-set-attribute -t unset '"+fileCompleteName+"' metadata::emblems") # Removes previous emblems.
		os.system("gvfs-set-attribute -t stringv '"+fileCompleteName+"' metadata::emblems $(gvfs-info '"
                    +fileCompleteName+"' | grep \"metadata::emblems:\" | sed s/\metadata::emblems:// | tr -d [,]) emblem-arrebol-exec-32")
		os.system("touch "+fileCompleteName); # Touch the file to update informations about emblems.
		
    def menu_activate_monitor(self, menu, selectedFile):
		os.system("echo 'Execution monitor' >> /tmp/log_menu.log")

		fileCompleteName = str(selectedFile.get_uri())[7:]

		os.system("gvfs-set-attribute -t unset '"+fileCompleteName+"' metadata::emblems") # Removes previous emblems.
		os.system("gvfs-set-attribute -t stringv '"+fileCompleteName+"' metadata::emblems $(gvfs-info '"
			+fileCompleteName+"' | grep \"metadata::emblems:\" | sed s/\metadata::emblems:// | tr -d [,]) emblem-arrebol-icon-32")
		os.system("touch "+fileCompleteName); # Touch the file to update informations about emblems.
		#selectedFile.add_emblem("emblem-arrebol-icon-32");
        
    def get_file_items(self, window, files):

		jdfExtension = ".jdf"
		#os.system("echo 'Context Menu Activated' > /tmp/log_menu.log")
		#os.system("echo '"+str(Gio.FileType.REGULAR)+"' > /tmp/log_menu.log")

		for selectedFile in files:
			fileType = selectedFile.get_file_type()
			#os.system("echo 'Files received "+str(file_type)+" ' >> /tmp/log_menu.log")
			#os.system("echo 'Name "+str(selectedFile.get_name())+" ' >> /tmp/log_menu.log")
			filename, file_extension = os.path.splitext(str(selectedFile.get_name()))
			#os.system("echo 'filename "+str(filename)+" ' >> /tmp/log_menu.log")
			#os.system("echo 'file_extension "+str(file_extension)+" ' >> /tmp/log_menu.log")

			if str(fileType) == str(Gio.FileType.REGULAR) and file_extension == jdfExtension:
				submenu = Nautilus.Menu()

				sub_menuitem_execute = Nautilus.MenuItem(name='ArrebolMenuProvider::execute_jdf', 
                                         label='Execute JDF', 
                                         tip='',
                                         icon='')
				sub_menuitem_execute.connect('activate', self.menu_activate_execute, selectedFile)
				submenu.append_item(sub_menuitem_execute)

				#sub_menuitem_monitor = Nautilus.MenuItem(name='ArrebolMenuProvider::monitor_jdf', 
                #                         label='Monitor JDF', 
                #                         tip='',
                #                         icon='')
				#sub_menuitem_monitor.connect('activate', self.menu_activate_monitor, selectedFile)
				#submenu.append_item(sub_menuitem_monitor)

				top_menuitem = Nautilus.MenuItem(name='ArrebolMenuProvider::Arrebol', 
	                                         label='Arrebol', 
	                                         tip='',
	                                         icon='')
		
				top_menuitem.set_submenu(submenu)

				return top_menuitem,
			else:
				os.system("echo 'Not Valid' >> /tmp/log_menu.log")
				return

		#for menuFile in files:        # Second Example
		#  self.fileName=fileName+" "+menuFile.get_file_type() 	

		#if len(files) != 1 or files[0].get_mime_type() != 'text/plain': return
		
		




		    def selectRadioHandle(self):



  , command=self.selectRadioHandle




  		#os.system("echo '"+str(self.credential)+" | bash "+str(self.ARREBOL_CLIENT_PATH)+" POST "+str(fileCompleteName)+" --username "+str(self.userName)+"' >> /tmp/log_menu.log")
		#os.system("echo "+str(self.credential)+" | bash "+str(self.ARREBOL_CLIENT_PATH)+" POST "+str(fileCompleteName)+" --username "+str(self.userName))
		#self.add_new_emblem("emblem-arrebol-exec-32", fileCompleteName)


				credential = self.userPassword.get()

		#if not self.userPassword:
    	#	credential = self.userPrivateKey
		#else:
    	#	credential = self.userPassword


    			#os.system("echo 'User: "+str(self.userName)+" -- credential: "+str(self.credential)+"' >> /tmp/log_menu.log")
		#os.system("echo '"+str(self.credential)+" | bash "+str(self.ARREBOL_CLIENT_PATH)+" POST "+str(fileCompleteName)+" --username "+str(self.userName)+"' >> /tmp/log_menu.log")
		#os.system("echo 'echo' "+str(self.credential)+" | bash "+str(self.ARREBOL_CLIENT_PATH)+" POST "+str(fileCompleteName)+" --username "+str(self.userName))